// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowGeocoder

private fun address(line: String?, lat: Double, lon: Double) =
    Address(Locale.ENGLISH).apply {
      if (line != null) setAddressLine(0, line)
      latitude = lat
      longitude = lon
    }

/**
 * The API 33 and above path, on the real [ShadowGeocoder], which serves the listener overload of
 * `getFromLocationName` from `setFromLocation` and `setErrorMessage`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class GeocodingRepositoryAndroidListenerTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val repository = GeocodingRepositoryAndroid(context)
  private val geocoder = Geocoder(context)

  private fun repositoryServing(addresses: List<Address>): GeocodingRepositoryAndroid {
    val platform = Geocoder(context, Locale.ENGLISH)
    shadowOf(platform).setFromLocation(addresses)
    return GeocodingRepositoryAndroid(platform)
  }

  @After fun tearDown() = ShadowGeocoder.reset()

  // Breaks if the address line is not the label: the list would show a bare country.
  @Test
  fun search_mapsAnAddressToItsLineAndCoordinates() = runTest {
    val result = repositoryServing(listOf(address("EPFL, Lausanne", 46.52, 6.57))).search("EPFL")

    assertEquals(listOf(GeocodingResult("EPFL, Lausanne", 46.52, 6.57)), result)
  }

  // Breaks if an address without a line is dropped or labelled "null": many places have no line.
  @Test
  fun search_withoutALineLabelsWithTheJoinedParts() = runTest {
    val noLine =
        address(null, 46.5, 6.6).apply {
          featureName = "Rolex"
          locality = "Lausanne"
          countryName = "Switzerland"
        }

    val result = repositoryServing(listOf(noLine)).search("Rolex")

    assertEquals("Rolex, Lausanne, Switzerland", result.single().label)
  }

  // Breaks if a nameless address reaches GeocodingResult, which would throw on its blank label.
  @Test
  fun search_skipsAnAddressWithNoLabel() = runTest {
    val result = repositoryServing(listOf(address(null, 1.0, 1.0))).search("nowhere")

    assertTrue(result.isEmpty())
  }

  // Breaks if the list is not trimmed to the limit when the backend returns more.
  @Test
  fun search_trimsToTheLimit() = runTest {
    val three = (1..3).map { address("Place $it", it.toDouble(), it.toDouble()) }

    val result = repositoryServing(three).search("place", limit = 2)

    assertEquals(listOf("Place 1", "Place 2"), result.map { it.label })
  }

  // Breaks if a listener error hangs the coroutine or loses the message.
  @Test
  fun search_onErrorBecomesAnIOException() = runTest {
    val platform = Geocoder(context, Locale.ENGLISH)
    shadowOf(platform).setErrorMessage("no network")

    try {
      GeocodingRepositoryAndroid(platform).search("EPFL")
      fail("expected IOException")
    } catch (e: IOException) {
      assertEquals("no network", e.message)
    }
  }

  // Breaks if a device without a backend throws instead of answering empty.
  @Test
  fun search_withoutABackendIsEmpty() = runTest {
    ShadowGeocoder.setIsPresent(false)

    assertTrue(repositoryServing(listOf(address("EPFL", 1.0, 1.0))).search("EPFL").isEmpty())
  }

  // Breaks if a blank query reaches the platform: it would be a wasted network call.
  @Test
  fun search_blankQueryIsEmpty() = runTest {
    shadowOf(geocoder).setErrorMessage("must not be called")

    assertTrue(repository.search("   ").isEmpty())
  }

  // Breaks if a limit below 1 is passed on to the platform, which rejects it with its own error.
  @Test(expected = IllegalArgumentException::class)
  fun search_limitBelowOneIsRefused() = runTest { repository.search("EPFL", limit = 0) }
}

/**
 * The API 28 to 32 path. The shadow does not serve the blocking `getFromLocationName`, so the
 * platform [Geocoder] is a mock.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class GeocodingRepositoryAndroidBlockingTest {
  private val geocoder = mockk<Geocoder>()
  private val repository = GeocodingRepositoryAndroid(geocoder)

  @After fun tearDown() = ShadowGeocoder.reset()

  // Breaks if the blocking overload is not used below API 33, where the listener one is absent.
  @Suppress("DEPRECATION")
  @Test
  fun search_mapsAnAddressToItsLineAndCoordinates() = runTest {
    every { geocoder.getFromLocationName("EPFL", 5) } returns
        listOf(address("EPFL, Lausanne", 46.52, 6.57))

    assertEquals(listOf(GeocodingResult("EPFL, Lausanne", 46.52, 6.57)), repository.search("EPFL"))
  }

  // Breaks if the limit is not trimmed on this path either.
  @Suppress("DEPRECATION")
  @Test
  fun search_trimsToTheLimit() = runTest {
    every { geocoder.getFromLocationName("place", 1) } returns
        listOf(address("Place 1", 1.0, 1.0), address("Place 2", 2.0, 2.0))

    assertEquals(listOf("Place 1"), repository.search("place", limit = 1).map { it.label })
  }

  // Breaks if a null list (the platform may return one) crashes instead of answering empty.
  @Suppress("DEPRECATION")
  @Test
  fun search_nullListIsEmpty() = runTest {
    every { geocoder.getFromLocationName("EPFL", 5) } returns null

    assertTrue(repository.search("EPFL").isEmpty())
  }

  // Breaks if the platform IOException is swallowed: the screen must be able to show the failure.
  @Suppress("DEPRECATION")
  @Test(expected = IOException::class)
  fun search_ioExceptionPropagates() = runTest {
    every { geocoder.getFromLocationName("EPFL", 5) } throws IOException("unreachable")

    repository.search("EPFL")
  }

  // Breaks if a blank query reaches the Geocoder.
  @Suppress("DEPRECATION")
  @Test
  fun search_blankQueryNeverTouchesTheGeocoder() = runTest {
    assertTrue(repository.search("  ").isEmpty())

    verify(exactly = 0) { geocoder.getFromLocationName(any<String>(), any()) }
  }

  // Breaks if a device without a backend still calls the Geocoder.
  @Suppress("DEPRECATION")
  @Test
  fun search_withoutABackendNeverTouchesTheGeocoder() = runTest {
    ShadowGeocoder.setIsPresent(false)

    assertTrue(repository.search("EPFL").isEmpty())

    verify(exactly = 0) { geocoder.getFromLocationName(any<String>(), any()) }
  }
}
