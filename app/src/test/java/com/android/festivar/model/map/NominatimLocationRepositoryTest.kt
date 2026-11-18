// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map

import com.android.festivar.model.map.geometry.GeoPosition
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** No request leaves the JVM: an interceptor answers in place of Nominatim. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NominatimLocationRepositoryTest {

  private val twoPlaces =
      """
      [
        {"lat": "46.5137", "lon": "6.6012", "display_name": "Parc de Vidy, Lausanne, Vaud"},
        {"lat": "46.5221982", "lon": "6.5661540", "display_name": "EPFL, Ecublens, Vaud"}
      ]
      """
          .trimIndent()

  private class Canned(private val code: Int, private val body: String) : Interceptor {
    val requests = mutableListOf<Request>()

    override fun intercept(chain: Interceptor.Chain): Response {
      requests += chain.request()
      return Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(code)
          .message(if (code == 200) "OK" else "Error")
          .body(body.toResponseBody())
          .build()
    }
  }

  /** A repository answered by [canned], whose clock is the virtual time of the test. */
  private fun TestScope.repository(canned: Canned) =
      NominatimLocationRepository(OkHttpClient.Builder().addInterceptor(canned).build()) {
        currentTime
      }

  /** Breaks if a place loses its name or its coordinates, or if the order of the answer changes. */
  @Test
  fun everyPlaceIsReadInTheOrderOfTheAnswer() = runTest {
    val found = repository(Canned(200, twoPlaces)).search("Vidy")

    assertEquals(
        listOf(
            Location("Parc de Vidy, Lausanne, Vaud", GeoPosition(46.5137, 6.6012)),
            Location("EPFL, Ecublens, Vaud", GeoPosition(46.5221982, 6.5661540)),
        ),
        found,
    )
  }

  /**
   * Breaks if the request does not go to the search of Nominatim, drops the query, or no longer
   * says who the application is, which the usage policy requires.
   */
  @Test
  fun theRequestIsTheNominatimSearchWithAUserAgent() = runTest {
    val canned = Canned(200, "[]")

    repository(canned).search("  Parc de Vidy, Lausanne ")

    val request = canned.requests.single()
    assertEquals("https", request.url.scheme)
    assertEquals("nominatim.openstreetmap.org", request.url.host)
    assertEquals(listOf("search"), request.url.pathSegments)
    assertEquals("Parc de Vidy, Lausanne", request.url.queryParameter("q"))
    assertEquals("json", request.url.queryParameter("format"))
    assertTrue(request.header("User-Agent").orEmpty().startsWith("FestivAR"))
  }

  /** Breaks if a blank query is sent to the service instead of being answered with nothing. */
  @Test
  fun aBlankQuerySendsNothing() = runTest {
    val canned = Canned(200, twoPlaces)

    assertEquals(emptyList<Location>(), repository(canned).search("   "))
    assertTrue(canned.requests.isEmpty())
  }

  /** Breaks if an answer without any place is not an empty list. */
  @Test
  fun noMatchIsAnEmptyList() = runTest {
    assertEquals(emptyList<Location>(), repository(Canned(200, "[]")).search("Nowhere"))
  }

  /**
   * Breaks if one place that cannot be read hides the others, or is returned half read: a latitude
   * that is not a number, a missing name, a latitude off the globe and a blank name are skipped.
   */
  @Test
  fun aPlaceThatCannotBeReadIsSkipped() = runTest {
    val json =
        """
        [
          {"lat": "oops", "lon": "6.5", "display_name": "Not a number"},
          {"lat": "46.5", "lon": "6.5"},
          {"lat": "95.0", "lon": "6.5", "display_name": "Off the globe"},
          {"lat": "46.5", "lon": "6.5", "display_name": " "},
          {"lat": "1.0", "lon": "2.0", "display_name": "Fine"}
        ]
        """
            .trimIndent()

    val found = repository(Canned(200, json)).search("x")

    assertEquals(listOf(Location("Fine", GeoPosition(1.0, 2.0))), found)
  }

  /** Breaks if an error of the service is swallowed and shown as "no place found". */
  @Test
  fun anErrorOfTheServiceIsAnIOException() {
    val exception =
        assertThrows(IOException::class.java) {
          runTest { repository(Canned(503, "busy")).search("Vidy") }
        }
    assertEquals("Nominatim answered 503.", exception.message)
  }

  /**
   * Breaks if an answer that is not a list of places crashes with something else than IOException.
   */
  @Test
  fun anAnswerThatIsNotAListIsAnIOException() {
    assertThrows(IOException::class.java) {
      runTest { repository(Canned(200, "<html>blocked</html>")).search("Vidy") }
    }
  }

  /**
   * Breaks if two searches reach the service within the same second, which the usage policy
   * forbids, or if the first search waits for nothing.
   */
  @Test
  fun twoSearchesAreOneSecondApart() = runTest {
    val repository = repository(Canned(200, "[]"))

    repository.search("a")
    assertEquals(0L, currentTime)
    repository.search("b")

    assertEquals(1_000L, currentTime)
  }

  /** Breaks if a search waits although the previous one is already more than a second old. */
  @Test
  fun aSearchAfterASecondDoesNotWait() = runTest {
    val repository = repository(Canned(200, "[]"))

    repository.search("a")
    delay(1_500)
    repository.search("b")

    assertEquals(1_500L, currentTime)
  }

  /**
   * Breaks if a search that is cancelled lets its request run to the end: a newer query would then
   * wait behind an answer nobody reads.
   */
  @Test
  fun cancellingASearchCancelsTheRequest() = runTest {
    val reached = CountDownLatch(1)
    val released = CountDownLatch(1)
    val finished = CountDownLatch(1)
    var cancelled = false
    val client =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
              reached.countDown()
              released.await(5, TimeUnit.SECONDS)
              cancelled = chain.call().isCanceled()
              finished.countDown()
              throw IOException("The test is over.")
            }
            .build()
    val search = launch(Dispatchers.IO) { NominatimLocationRepository(client).search("Vidy") }
    withContext(Dispatchers.IO) { assertTrue(reached.await(5, TimeUnit.SECONDS)) }

    search.cancelAndJoin()
    released.countDown()

    withContext(Dispatchers.IO) { assertTrue(finished.await(5, TimeUnit.SECONDS)) }
    assertTrue(cancelled)
  }
}
