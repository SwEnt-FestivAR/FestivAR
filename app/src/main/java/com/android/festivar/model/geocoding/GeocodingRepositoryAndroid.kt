// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * [GeocodingRepository] over the platform [Geocoder]: free and without an API key, and good enough
 * because the organizer confirms the venue on the map afterwards.
 *
 * @param geocoder The platform geocoder to ask.
 */
class GeocodingRepositoryAndroid internal constructor(private val geocoder: Geocoder) :
    GeocodingRepository {

  /**
   * @param context The context the [Geocoder] is built from; pass the application context.
   * @param locale The language the places are named in.
   */
  constructor(
      context: Context,
      locale: Locale = Locale.getDefault(),
  ) : this(Geocoder(context, locale))

  override suspend fun search(query: String, limit: Int): List<GeocodingResult> {
    require(limit >= 1) { "limit must be at least 1, was $limit" }
    if (query.isBlank() || !Geocoder.isPresent()) return emptyList()
    val addresses =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(
                query,
                limit,
                object : Geocoder.GeocodeListener {
                  override fun onGeocode(addresses: MutableList<Address>) {
                    continuation.resume(addresses)
                  }

                  override fun onError(errorMessage: String?) {
                    continuation.resumeWithException(
                        IOException(errorMessage ?: "Geocoding failed")
                    )
                  }
                },
            )
          }
        } else {
          // The listener overload only exists from API 33 and minSdk is 28, so older devices take
          // the blocking overload, off the main thread.
          withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION") geocoder.getFromLocationName(query, limit)
          }
        }
    return (addresses ?: emptyList()).mapNotNull(::toResult).take(limit)
  }

  private fun toResult(address: Address): GeocodingResult? {
    val label =
        if (address.maxAddressLineIndex >= 0) address.getAddressLine(0)
        else
            listOfNotNull(address.featureName, address.locality, address.countryName)
                .joinToString(", ")
    if (label.isNullOrBlank() || !address.hasLatitude() || !address.hasLongitude()) return null
    return GeocodingResult(label, address.latitude, address.longitude)
  }
}
