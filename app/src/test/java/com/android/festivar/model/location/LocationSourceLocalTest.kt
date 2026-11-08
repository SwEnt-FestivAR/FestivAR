// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.location

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationSourceLocalTest {

  @Test
  fun updatesEmitsTheGivenFixesInOrder() = runTest {
    val fixes =
        listOf(
            LocationFix(46.5197, 6.6323, 5f),
            LocationFix(46.5198, 6.6324, 3f),
            LocationFix(46.5199, 6.6325, 8f),
        )

    val source = LocationSourceLocal(flowOf(*fixes.toTypedArray()))

    assertEquals(fixes, source.updates().toList())
  }

  @Test
  fun anEmptySourceCompletesWithoutAValue() = runTest {
    assertEquals(emptyList<LocationFix>(), LocationSourceLocal(emptyFlow()).updates().toList())
  }
}
