// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import android.Manifest
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.voicenote.VoiceNoteCallbacks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceNoteMicrophoneTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val controller = mockk<VoiceNoteController>(relaxed = true)
  private lateinit var callbacks: VoiceNoteCallbacks
  private val startedIntents = mutableListOf<Intent>()

  @Before
  fun setUp() {
    io.mockk.every { controller.callbacks(any(), any(), any()) } answers
        {
          val hasMicrophone = firstArg<() -> Boolean>()
          val requestMicrophone = secondArg<() -> Unit>()
          val onOpenSettings = thirdArg<() -> Unit>()
          VoiceNoteCallbacks(
              onTogglePlay = {},
              onDelete = {},
              onConfirmDelete = {},
              onDismissDelete = {},
              onRecordStart = {
                if (hasMicrophone()) controller.startRecording() else requestMicrophone()
              },
              onRecordRelease = {},
              onConfirmSend = {},
              onCancelSend = {},
              onDismissRecordingFailed = {},
              onRetry = {},
              onOpenSettings = onOpenSettings,
          )
        }
  }

  private fun showMicrophone(hasMicrophone: Boolean) {
    val context =
        object : ContextWrapper(composeTestRule.activity) {
          override fun checkSelfPermission(permission: String) =
              if (permission == Manifest.permission.RECORD_AUDIO && hasMicrophone) {
                PackageManager.PERMISSION_GRANTED
              } else {
                PackageManager.PERMISSION_DENIED
              }

          override fun checkPermission(permission: String, pid: Int, uid: Int) =
              checkSelfPermission(permission)

          override fun startActivity(intent: Intent) {
            startedIntents += intent
          }
        }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalContext provides context) {
        callbacks = rememberVoiceNoteCallbacks(controller = controller, micDenied = false)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun recordingStartsWhenMicrophonePermissionIsGranted() {
    showMicrophone(hasMicrophone = true)

    callbacks.onRecordStart()

    verify { controller.startRecording() }
  }

  @Test
  fun recordingDoesNotStartWhenMicrophonePermissionIsDenied() {
    showMicrophone(hasMicrophone = false)

    callbacks.onRecordStart()

    verify(exactly = 0) { controller.startRecording() }
  }

  @Test
  fun openingSettingsLaunchesTheApplicationDetailsPage() {
    showMicrophone(hasMicrophone = false)

    callbacks.onOpenSettings()

    val intent = startedIntents.single()
    assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
    assertEquals(
        "package:${composeTestRule.activity.packageName}",
        intent.dataString,
    )
  }
}
