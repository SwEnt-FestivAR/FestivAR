// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.ar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

  /**
   * Breaks if the check hands ARCore the context it was built with instead of the application
   * context: built from a screen, it would keep that screen alive after it is closed.
   */
  @Test
  fun theCheckOnlyTakesTheApplicationContextOfTheOneItIsGiven() = runTest {
    val application = ApplicationProvider.getApplicationContext<Context>()
    val screen = mockk<Context>()
    every { screen.applicationContext } returns application

    ArCoreSupport(screen).check()

    verify(exactly = 1) { screen.applicationContext }
    confirmVerified(screen)
  }
}
