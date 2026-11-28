// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.ar

import com.android.festivar.model.ar.ArAvailability
import com.android.festivar.model.ar.ArSupport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArEntryViewModelTest {

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun viewModel(answer: ArAvailability) = ArEntryViewModel(ArSupport { answer })

  /** Breaks if the screen leaves the loading state before the availability check has answered. */
  @Test
  fun theStateIsCheckingWhileTheAvailabilityCheckRuns() {
    val answer = CompletableDeferred<ArAvailability>()
    val vm =
        ArEntryViewModel(
            object : ArSupport {
              override suspend fun check() = answer.await()
            }
        )
    vm.onCameraChecked(granted = true)

    assertEquals(ArEntryUiState.Checking, vm.uiState.value)

    answer.complete(ArAvailability.SUPPORTED)
    assertEquals(ArEntryUiState.Ready, vm.uiState.value)
  }

  /** Breaks if the state is decided before the route has said whether the camera is granted. */
  @Test
  fun theStateIsCheckingUntilTheCameraPermissionIsKnown() {
    val vm = viewModel(ArAvailability.SUPPORTED)

    assertEquals(ArEntryUiState.Checking, vm.uiState.value)
  }

  /** Breaks if a supported phone with the camera granted does not go straight to the AR content. */
  @Test
  fun aSupportedPhoneWithTheCameraGrantedIsReady() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = true)

    assertEquals(ArEntryUiState.Ready, vm.uiState.value)
  }

  /**
   * Breaks if a phone that lacks the AR services is let into the AR view, which cannot start
   * without them, or is told that it cannot do AR at all.
   */
  @Test
  fun aPhoneThatLacksTheArServicesIsAskedToInstallThemWhateverTheCamera() {
    val granted = viewModel(ArAvailability.NEEDS_INSTALL)
    granted.onCameraChecked(granted = true)
    val unknown = viewModel(ArAvailability.NEEDS_INSTALL)

    assertEquals(ArEntryUiState.NeedsInstall, granted.uiState.value)
    assertEquals(ArEntryUiState.NeedsInstall, unknown.uiState.value)
  }

  /** Breaks if coming back from the install with the AR services there does not go on. */
  @Test
  fun checkingAgainAfterTheInstallGoesOn() {
    val vm =
        ArEntryViewModel(ArSupportSequence(ArAvailability.NEEDS_INSTALL, ArAvailability.SUPPORTED))
    vm.onCameraChecked(granted = true)
    assertEquals(ArEntryUiState.NeedsInstall, vm.uiState.value)

    vm.retry()

    assertEquals(ArEntryUiState.Ready, vm.uiState.value)
  }

  /** Breaks if an install the user cancelled is forgotten, and the AR view starts without it. */
  @Test
  fun checkingAgainAfterACancelledInstallStillAsksForIt() {
    val vm = ArEntryViewModel(ArSupportSequence(ArAvailability.NEEDS_INSTALL))
    vm.onCameraChecked(granted = true)

    vm.retry()

    assertEquals(ArEntryUiState.NeedsInstall, vm.uiState.value)
  }

  /** Breaks if a phone that cannot run AR asks for the camera instead of offering the map. */
  @Test
  fun anUnsupportedPhoneIsUnavailableWhateverTheCamera() {
    val granted = viewModel(ArAvailability.UNSUPPORTED)
    granted.onCameraChecked(granted = true)
    val notGranted = viewModel(ArAvailability.UNSUPPORTED)
    notGranted.onCameraChecked(granted = false)

    assertEquals(ArEntryUiState.Unavailable(canRetry = false), granted.uiState.value)
    assertEquals(ArEntryUiState.Unavailable(canRetry = false), notGranted.uiState.value)
  }

  /** Breaks if an unsupported phone shows the loading state until the camera is known. */
  @Test
  fun anUnsupportedPhoneIsUnavailableBeforeTheCameraIsKnown() {
    val vm = viewModel(ArAvailability.UNSUPPORTED)

    assertEquals(ArEntryUiState.Unavailable(canRetry = false), vm.uiState.value)
  }

  /**
   * Breaks if a failing check leaves the screen loading forever, or says for good that the phone
   * cannot do AR instead of offering to try again.
   */
  @Test
  fun aCheckThatThrowsIsUnavailableWithARetry() {
    val vm =
        ArEntryViewModel(
            object : ArSupport {
              override suspend fun check(): ArAvailability = throw IllegalStateException("boom")
            }
        )
    vm.onCameraChecked(granted = true)

    assertEquals(ArEntryUiState.Unavailable(canRetry = true), vm.uiState.value)
  }

  /** Breaks if a supported phone without the camera does not ask for it. */
  @Test
  fun aSupportedPhoneWithoutTheCameraAsksForIt() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = false)

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = false), vm.uiState.value)
  }

  /** Breaks if granting the camera in the system dialog does not move on to the AR content. */
  @Test
  fun grantingTheCameraMakesItReady() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = false)

    vm.onCameraAnswered(
        granted = true,
        rationaleBefore = false,
        rationaleAfter = false,
        answeredAtOnce = false,
    )

    assertEquals(ArEntryUiState.Ready, vm.uiState.value)
  }

  /** Breaks if a refusal the system will ask about again is taken as a refusal for good. */
  @Test
  fun refusingOnceKeepsAskingForTheCamera() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = false)

    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = false,
        rationaleAfter = true,
        answeredAtOnce = false,
    )

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = false), vm.uiState.value)
  }

  /**
   * Breaks if a refusal for good keeps launching a system dialog that no longer shows: the system
   * recommended a rationale before the request and no longer does after it.
   */
  @Test
  fun refusingForGoodSendsTheUserToTheSettings() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = false)

    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = true,
        rationaleAfter = false,
        answeredAtOnce = false,
    )

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = true), vm.uiState.value)
  }

  /**
   * Breaks if a dialog dismissed without an answer is taken for a refusal for good: no rationale
   * before and none after, exactly like one, but the user took the time to read it, and the dialog
   * would show again.
   */
  @Test
  fun aDismissedDialogIsNotARefusalForGood() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = false)

    repeat(2) {
      vm.onCameraAnswered(
          granted = false,
          rationaleBefore = false,
          rationaleAfter = false,
          answeredAtOnce = false,
      )
    }

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = false), vm.uiState.value)
  }

  /**
   * Breaks if a camera refused for good in an earlier run leaves the user tapping a button that
   * shows nothing: the refusal comes back at once, with no dialog, and sends to the settings from
   * the first tap.
   */
  @Test
  fun aRefusalThatComesBackAtOnceIsForGood() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = false)

    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = false,
        rationaleAfter = false,
        answeredAtOnce = true,
    )

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = true), vm.uiState.value)
  }

  /**
   * Breaks if a quick tap on "Deny" is taken for a refusal for good: the system still recommends a
   * rationale after it, so the dialog shows again.
   */
  @Test
  fun aQuickRefusalTheSystemAsksAboutAgainIsNotForGood() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraChecked(granted = false)

    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = false,
        rationaleAfter = true,
        answeredAtOnce = true,
    )

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = false), vm.uiState.value)
  }

  /** Breaks if a grant keeps an earlier refusal for good, and a later check shows the settings. */
  @Test
  fun aGrantForgetsARefusalForGood() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = true,
        rationaleAfter = false,
        answeredAtOnce = false,
    )
    // The system recommended a rationale before this request and, once granted, no longer does:
    // the same signs as a refusal for good, on an answer that is a grant.
    vm.onCameraAnswered(
        granted = true,
        rationaleBefore = true,
        rationaleAfter = false,
        answeredAtOnce = false,
    )

    vm.onCameraChecked(granted = false)

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = false), vm.uiState.value)
  }

  /** Breaks if an ordinary refusal after a refusal for good still sends to the settings. */
  @Test
  fun aRefusalTheSystemAsksAboutAgainIsNoLongerForGood() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = true,
        rationaleAfter = false,
        answeredAtOnce = false,
    )

    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = true,
        rationaleAfter = true,
        answeredAtOnce = false,
    )

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = false), vm.uiState.value)
  }

  /** Breaks if coming back from the settings without the camera forgets the refusal for good. */
  @Test
  fun aRecheckStillWithoutTheCameraKeepsTheRefusalForGood() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = true,
        rationaleAfter = false,
        answeredAtOnce = false,
    )

    vm.onCameraChecked(granted = false)

    assertEquals(ArEntryUiState.NeedsCamera(refusedForGood = true), vm.uiState.value)
  }

  /** Breaks if granting the camera in the settings is not noticed when the user comes back. */
  @Test
  fun grantingTheCameraInTheSettingsMakesItReady() {
    val vm = viewModel(ArAvailability.SUPPORTED)
    vm.onCameraAnswered(
        granted = false,
        rationaleBefore = true,
        rationaleAfter = false,
        answeredAtOnce = false,
    )

    vm.onCameraChecked(granted = true)

    assertEquals(ArEntryUiState.Ready, vm.uiState.value)
  }

  /** An [ArSupport] giving [answers] one per check, the last one again once they run out. */
  private class ArSupportSequence(private vararg val answers: ArAvailability) : ArSupport {
    var checks = 0

    override suspend fun check(): ArAvailability = answers[minOf(checks++, answers.size - 1)]
  }

  /**
   * Breaks if a check that could not tell is shown as a phone that cannot do AR, without a retry.
   */
  @Test
  fun aCheckThatCouldNotTellIsUnavailableWithARetry() {
    val vm = viewModel(ArAvailability.UNKNOWN)
    vm.onCameraChecked(granted = true)

    assertEquals(ArEntryUiState.Unavailable(canRetry = true), vm.uiState.value)
  }

  /** Breaks if trying again does not run the check again, or ignores its new answer. */
  @Test
  fun tryingAgainAfterCouldNotTellCanReachReady() {
    val support = ArSupportSequence(ArAvailability.UNKNOWN, ArAvailability.SUPPORTED)
    val vm = ArEntryViewModel(support)
    vm.onCameraChecked(granted = true)

    vm.retry()

    assertEquals(2, support.checks)
    assertEquals(ArEntryUiState.Ready, vm.uiState.value)
  }

  /** Breaks if a second answer that still could not tell loses the retry. */
  @Test
  fun couldNotTellTwiceStillOffersARetry() {
    val support = ArSupportSequence(ArAvailability.UNKNOWN)
    val vm = ArEntryViewModel(support)
    vm.onCameraChecked(granted = false)

    vm.retry()

    assertEquals(2, support.checks)
    assertEquals(ArEntryUiState.Unavailable(canRetry = true), vm.uiState.value)
  }

  /** Breaks if the screen keeps the old answer on show while the new check runs. */
  @Test
  fun tryingAgainShowsCheckingUntilTheNewAnswer() {
    val answer = CompletableDeferred<ArAvailability>()
    var checks = 0
    val vm =
        ArEntryViewModel(
            object : ArSupport {
              override suspend fun check() =
                  if (checks++ == 0) ArAvailability.UNKNOWN else answer.await()
            }
        )
    vm.onCameraChecked(granted = true)

    vm.retry()

    assertEquals(ArEntryUiState.Checking, vm.uiState.value)
    answer.complete(ArAvailability.UNSUPPORTED)
    assertEquals(ArEntryUiState.Unavailable(canRetry = false), vm.uiState.value)
  }
}
