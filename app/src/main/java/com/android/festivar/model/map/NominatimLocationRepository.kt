// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map

import android.util.Log
import com.android.festivar.model.map.geometry.GeoPosition
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException

private const val TAG = "NominatimLocationRepo"

/** The usage policy of Nominatim asks every application to say who it is. */
private const val USER_AGENT = "FestivAR (https://github.com/SwEnt-FestivAR/FestivAR)"

/** The usage policy of Nominatim allows one request per second at most. */
private const val MIN_INTERVAL_MILLIS = 1_000L

/**
 * A [LocationRepository] on the search API of Nominatim, the geocoder of OpenStreetMap. It needs no
 * API key.
 *
 * Requests of one instance are kept one second apart, as the usage policy asks, so the application
 * should share a single instance.
 *
 * @param client The HTTP client the requests go through.
 * @param now A clock in milliseconds that never goes back, so that a correction of the time of the
 *   device cannot make a search wait; a test replaces it to control the waiting.
 */
class NominatimLocationRepository(
    private val client: OkHttpClient,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) : LocationRepository {

  private val throttle = Mutex()
  private var lastRequestAt: Long? = null

  override suspend fun search(query: String): List<Location> {
    if (query.isBlank()) return emptyList()
    val url =
        HttpUrl.Builder()
            .scheme("https")
            .host("nominatim.openstreetmap.org")
            .addPathSegment("search")
            .addQueryParameter("q", query.trim())
            .addQueryParameter("format", "json")
            .build()
    val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
    waitForOurTurn()
    return parseLocations(client.newCall(request).awaitBody())
  }

  /**
   * The body of the answer to this call, read on the thread of OkHttp so that the caller is never
   * blocked. Cancelling the search cancels the request.
   *
   * @throws IOException if the service cannot be reached or answers with an error.
   */
  private suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
          override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
          }

          override fun onResponse(call: Call, response: Response) {
            try {
              val body = response.use {
                if (!it.isSuccessful) throw IOException("Nominatim answered ${it.code}.")
                it.body.string()
              }
              continuation.resume(body)
            } catch (e: IOException) {
              continuation.resumeWithException(e)
            }
          }
        }
    )
  }

  /** Waits until one second has passed since the previous request of this instance. */
  private suspend fun waitForOurTurn() = throttle.withLock {
    lastRequestAt?.let { last ->
      val wait = last + MIN_INTERVAL_MILLIS - now()
      if (wait > 0) delay(wait)
    }
    lastRequestAt = now()
  }

  /**
   * The places of an answer of Nominatim. A place that cannot be read is skipped, so that one bad
   * entry does not hide the others.
   *
   * @throws IOException if the answer as a whole is not the expected JSON array.
   */
  private fun parseLocations(json: String): List<Location> {
    val places =
        try {
          JSONArray(json)
        } catch (e: JSONException) {
          throw IOException("Nominatim answered something that is not a list of places.", e)
        }
    return (0 until places.length()).mapNotNull { index ->
      try {
        val place = places.getJSONObject(index)
        Location(
            name = place.getString("display_name"),
            position =
                GeoPosition(
                    latitude = place.getString("lat").toDouble(),
                    longitude = place.getString("lon").toDouble(),
                ),
        )
      } catch (e: JSONException) {
        skipped(e)
      } catch (e: IllegalArgumentException) {
        skipped(e)
      }
    }
  }

  private fun skipped(cause: Exception): Location? {
    Log.w(TAG, "Skipped a place that could not be read.", cause)
    return null
  }
}
