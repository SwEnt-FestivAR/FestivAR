// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.ar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.ar.ArAvailability
import com.android.festivar.model.ar.ArSupport
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val LOG_TAG = "ArEntryViewModel"

/** What the way into AR shows. */
sealed interface ArEntryUiState {
  /** The availability of AR or the camera permission is not known yet. */
  data object Checking : ArEntryUiState

  /**
   * The phone can run AR once Google Play Services for AR is installed or updated. The camera is
   * asked for after that.
   */
  data object NeedsInstall : ArEntryUiState

  /**
   * AR can run but the camera is not granted.
   *
   * @property refusedForGood the system no longer shows its dialog, so the camera can only be
   *   granted in the app settings.
   */
  data class NeedsCamera(val refusedForGood: Boolean) : ArEntryUiState

  /**
   * AR cannot be shown; the map is offered instead.
   *
   * @property canRetry the check could not tell whether this phone can do AR, so it may be run
   *   again. False means the phone cannot do AR.
   */
  data class Unavailable(val canRetry: Boolean) : ArEntryUiState

  /** AR can run and the camera is granted. */
  data object Ready : ArEntryUiState
}

/**
 * Decides whether the user may go into AR: it asks [arSupport] when built and again on [retry], and
 * the route tells it about the camera permission. AR that cannot run gives
 * [ArEntryUiState.Unavailable] whatever the camera; a check that could not tell or that throws
 * gives it with a retry. A phone that lacks Google Play Services for AR gives
 * [ArEntryUiState.NeedsInstall], so that the AR view never starts without them. Otherwise the state
 * is [ArEntryUiState.Checking] until both answers are in, then [ArEntryUiState.Ready] with the
 * camera granted or [ArEntryUiState.NeedsCamera] without it.
 */
class ArEntryViewModel(private val arSupport: ArSupport) : ViewModel() {
  private var availability: ArAvailability? = null
  private var cameraGranted: Boolean? = null
  private var refusedForGood = false

  private val _uiState = MutableStateFlow<ArEntryUiState>(ArEntryUiState.Checking)

  /** The one state of the screen. */
  val uiState: StateFlow<ArEntryUiState> = _uiState.asStateFlow()

  init {
    check()
  }

  /**
   * Runs the availability check again: after it could not tell, or when the user comes back from
   * the install of Google Play Services for AR.
   */
  fun retry() {
    availability = null
    publish()
    check()
  }

  private fun check() {
    viewModelScope.launch {
      availability =
          try {
            arSupport.check()
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            Log.w(LOG_TAG, "AR availability check failed", e)
            ArAvailability.UNKNOWN
          }
      publish()
    }
  }

  /**
   * Whether the camera is granted right now, read by the route when the screen shows or comes back
   * from the settings. It keeps an earlier refusal for good while the camera is still not granted.
   */
  fun onCameraChecked(granted: Boolean) {
    cameraGranted = granted
    publish()
  }

  /**
   * The answer to a camera request. [rationaleBefore] and [rationaleAfter] are what the system said
   * about showing a rationale before the request and after the answer, and [answeredAtOnce] is
   * whether the answer came back too fast for anyone to have read a dialog.
   *
   * A refusal is for good, so that the user is sent to the settings, when the system stopped
   * recommending a rationale (true before, false after: this refusal was the last the system asks
   * about), or when it came back at once with no rationale, which means no dialog showed. A dialog
   * the user dismissed without answering also has no rationale on either side, but it took its
   * time, and it shows again the next time.
   */
  fun onCameraAnswered(
      granted: Boolean,
      rationaleBefore: Boolean,
      rationaleAfter: Boolean,
      answeredAtOnce: Boolean,
  ) {
    cameraGranted = granted
    refusedForGood = !granted && !rationaleAfter && (rationaleBefore || answeredAtOnce)
    publish()
  }

  private fun publish() {
    _uiState.value =
        when {
          availability == ArAvailability.UNSUPPORTED -> ArEntryUiState.Unavailable(canRetry = false)
          availability == ArAvailability.UNKNOWN -> ArEntryUiState.Unavailable(canRetry = true)
          availability == ArAvailability.NEEDS_INSTALL -> ArEntryUiState.NeedsInstall
          availability == null || cameraGranted == null -> ArEntryUiState.Checking
          cameraGranted == true -> ArEntryUiState.Ready
          else -> ArEntryUiState.NeedsCamera(refusedForGood)
        }
  }
}
