// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.geocoding

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingRepositoryLocalTest {
  private val rolex = GeocodingResult("Rolex Learning Center, Lausanne", 46.5187, 6.5681)
  private val flon = GeocodingResult("Flon, Lausanne", 46.5214, 6.6249)
  private val bern = GeocodingResult("Bern", 46.948, 7.4474)
  private val repository =
      GeocodingRepositoryLocal(mapOf("lausanne" to listOf(rolex, flon), "bern" to listOf(bern)))

  // Breaks if the match is case sensitive: "LAUSANNE" would find nothing.
  @Test
  fun search_matchesAKeyInsideTheQueryIgnoringCase() = runTest {
    assertEquals(listOf(bern), repository.search("Rue de Berne, BERN"))
  }

  // Breaks if the order is shuffled: the best place must stay first.
  @Test
  fun search_keepsTheResultsInOrder() = runTest {
    assertEquals(listOf(rolex, flon), repository.search("Lausanne"))
  }

  // Breaks if the limit is ignored: the picker would list more than it asked for.
  @Test
  fun search_appliesTheLimit() = runTest {
    assertEquals(listOf(rolex), repository.search("Lausanne", limit = 1))
  }

  // Breaks if a blank query reaches the matcher: an empty key would match everything.
  @Test
  fun search_blankQueryIsEmpty() = runTest {
    val everything = GeocodingRepositoryLocal(mapOf("" to listOf(bern)))
    assertTrue(everything.search("   ").isEmpty())
  }

  // Breaks if an unknown address invents a place.
  @Test
  fun search_unknownQueryIsEmpty() = runTest { assertTrue(repository.search("Zurich").isEmpty()) }

  // Breaks if the fake accepts a limit the real repository refuses.
  @Test(expected = IllegalArgumentException::class)
  fun search_limitBelowOneIsRefused() = runTest { repository.search("Lausanne", limit = 0) }
}
