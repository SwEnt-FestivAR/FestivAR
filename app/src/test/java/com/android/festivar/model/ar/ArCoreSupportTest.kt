// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.ar

import com.google.ar.core.ArCoreApk.Availability
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ArCoreSupportTest {

  /** Polls a fake ARCore that gives [answers] in turn, then repeats the last one. */
  private suspend fun answerOf(vararg answers: Availability): Pair<ArAvailability, Int> {
    var calls = 0
    val result = pollArCore {
      calls++
      answers[minOf(calls, answers.size) - 1]
    }
    return result to calls
  }

  /** Breaks if a phone with ARCore installed is not taken as supported. */
  @Test
  fun installedArCoreIsSupported() = runTest {
    assertEquals(ArAvailability.SUPPORTED, answerOf(Availability.SUPPORTED_INSTALLED).first)
  }

  /** Breaks if a capable phone missing ARCore, or with an old one, is not offered the install. */
  @Test
  fun missingOrOldArCoreNeedsAnInstall() = runTest {
    assertEquals(
        ArAvailability.NEEDS_INSTALL,
        answerOf(Availability.SUPPORTED_NOT_INSTALLED).first,
    )
    assertEquals(ArAvailability.NEEDS_INSTALL, answerOf(Availability.SUPPORTED_APK_TOO_OLD).first)
  }

  /** Breaks if a phone that cannot run AR is taken as able to. */
  @Test
  fun anIncapablePhoneIsUnsupported() = runTest {
    assertEquals(
        ArAvailability.UNSUPPORTED,
        answerOf(Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE).first,
    )
  }

  /** Breaks if a phone that was offline or hit an error is told for good it cannot do AR. */
  @Test
  fun anErrorOrATimeoutCouldNotTell() = runTest {
    assertEquals(ArAvailability.UNKNOWN, answerOf(Availability.UNKNOWN_ERROR).first)
    assertEquals(ArAvailability.UNKNOWN, answerOf(Availability.UNKNOWN_TIMED_OUT).first)
  }

  /** Breaks if the transient "checking" answer is taken as final instead of asked again. */
  @Test
  fun checkingIsAskedAgainUntilTheRealAnswer() = runTest {
    val (answer, calls) =
        answerOf(
            Availability.UNKNOWN_CHECKING,
            Availability.UNKNOWN_CHECKING,
            Availability.SUPPORTED_INSTALLED,
        )

    assertEquals(ArAvailability.SUPPORTED, answer)
    assertEquals(3, calls)
  }

  /**
   * Breaks if a check that never settles waits forever, or says the phone cannot do AR when it only
   * could not tell.
   */
  @Test
  fun checkingForeverCouldNotTellAfterTheBoundedRetries() = runTest {
    val (answer, calls) = answerOf(Availability.UNKNOWN_CHECKING)

    assertEquals(ArAvailability.UNKNOWN, answer)
    assertEquals(ARCORE_CHECK_ATTEMPTS, calls)
  }
}
