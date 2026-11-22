// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.ar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real [ArCoreSupport] against ARCore itself, in the unit test environment, where there is
 * no Play Store and Google Play Services for AR is not installed.
 */
@RunWith(AndroidJUnit4::class)
class ArCoreSupportOnDeviceTest {

  /**
   * Breaks if the real check throws or never returns where ARCore cannot answer, or if it then says
   * AR can start or only needs an install: the app would open an AR screen that cannot work.
   */
  @Test
  fun whereArCoreCannotAnswerArIsNotOffered() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val answer = ArCoreSupport(context).check()

    assertTrue(
        "expected UNKNOWN or UNSUPPORTED but was $answer",
        answer == ArAvailability.UNKNOWN || answer == ArAvailability.UNSUPPORTED,
    )
  }
}
