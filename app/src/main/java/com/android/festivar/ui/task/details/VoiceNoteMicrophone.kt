// Co-authored-by: Copilot App
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.festivar.ui.voicenote.VoiceNoteCallbacks

/**
 * The actions of the voice note section, with the parts that need the activity: asking for the
 * microphone permission and opening the settings of the app.
 *
 * The permission is asked on the first press of the record button. If it is refused, the section
 * shows the microphone as off ([micDenied]) with a button to the settings, and the permission is
 * checked again when the user comes back from there.
 *
 * @param controller the voice note of the task, which answers the actions.
 * @param micDenied whether the section shows the microphone as off.
 */
@Composable
internal fun rememberVoiceNoteCallbacks(
    controller: VoiceNoteController,
    micDenied: Boolean,
): VoiceNoteCallbacks {
  val context = LocalContext.current
  val request =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        controller.onMicPermissionResult(granted)
      }
  // The user may have turned the microphone on in the settings
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    if (micDenied) controller.onMicPermissionResult(context.hasMicrophone())
  }
  return remember(controller, context, request) {
    controller.callbacks(
        hasMicrophone = { context.hasMicrophone() },
        requestMicrophone = { request.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = { context.openAppSettings() },
    )
  }
}

private fun Context.hasMicrophone() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() =
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
    )

