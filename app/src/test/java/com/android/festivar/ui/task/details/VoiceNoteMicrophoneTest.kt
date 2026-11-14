// Co-authored-by: Copilot App
package com.android.festivar.ui.task.details

import android.Manifest
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import com.android.festivar.ui.voicenote.VoiceNoteCallbacks
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceNoteMicrophoneTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var controller: VoiceNoteController
  private lateinit var recorder: FakeVoiceRecorder
  private lateinit var callbacks: VoiceNoteCallbacks
  private val startedIntents = mutableListOf<Intent>()
  private val activityResultRegistry = TestActivityResultRegistry()

  @Before
  fun setUp() {
    val host = TestVoiceNoteHost()
    recorder = FakeVoiceRecorder()
    controller =
        spyk(
            VoiceNoteController(
                CoroutineScope(Dispatchers.Unconfined),
                host,
                VoiceNoteServices(recorder, FakeAudioRepository(), FakeVoicePlayer()),
            ),
        )
  }

  private fun showMicrophone(hasMicrophone: Boolean, micDenied: Boolean = false) {
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
      CompositionLocalProvider(
          LocalContext provides context,
          LocalActivityResultRegistryOwner provides
              object : androidx.activity.result.ActivityResultRegistryOwner {
                override val activityResultRegistry =
                    this@VoiceNoteMicrophoneTest.activityResultRegistry
              },
      ) {
        callbacks = rememberVoiceNoteCallbacks(controller = controller, micDenied = micDenied)
      }
    }
    composeTestRule.waitForIdle()
  }

  /** Breaks if an enabled microphone does not start recording through the real controller. */
  @Test
  fun recordingStartsWhenMicrophonePermissionIsGranted() {
    showMicrophone(hasMicrophone = true)

    callbacks.onRecordStart()

    assertEquals(1, recorder.starts)
  }

  /** Breaks if a denied microphone starts recording instead of launching the permission request. */
  @Test
  fun recordingDoesNotStartWhenMicrophonePermissionIsDenied() {
    showMicrophone(hasMicrophone = false)

    callbacks.onRecordStart()

    assertEquals(0, recorder.starts)
  }

  /** Breaks if the permission launcher result is not forwarded to the controller. */
  @Test
  fun permissionResultIsForwardedToTheController() {
    showMicrophone(hasMicrophone = false)

    callbacks.onRecordStart()
    activityResultRegistry.dispatchPermissionResult(granted = true)

    io.mockk.verify { controller.onMicPermissionResult(true) }
  }

  /** Breaks if returning from settings does not re-check a previously denied microphone. */
  @Test
  fun returningFromSettings_rechecksMicrophonePermission() {
    showMicrophone(hasMicrophone = true, micDenied = true)

    composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
    composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

    verify { controller.onMicPermissionResult(true) }
  }

  /** Breaks if an allowed microphone triggers a spurious permission result on resume. */
  @Test
  fun returningFromSettings_doesNothingWhenMicrophoneWasNotDenied() {
    showMicrophone(hasMicrophone = true, micDenied = false)

    composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
    composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

    verify(exactly = 0) { controller.onMicPermissionResult(any()) }
  }

  /** Breaks if opening settings does not target this application's details page. */
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

  private class TestActivityResultRegistry : ActivityResultRegistry() {
    private var requestCode: Int? = null

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: androidx.core.app.ActivityOptionsCompat?,
    ) {
      this.requestCode = requestCode
    }

    fun dispatchPermissionResult(granted: Boolean) {
      dispatchResult(requireNotNull(requestCode), granted)
    }
  }

  private class TestVoiceNoteHost : VoiceNoteHost {
    private val task = Task("task", "event", "Task", ownerId = "owner")
    override val state: StateFlow<TaskDetailsUiState> =
        MutableStateFlow(
            TaskDetailsUiState(
                currentUserId = "owner",
                task = task,
                isLoading = false,
            ),
        )

    override fun startSaving(allowed: (TaskDetailsUiState) -> Boolean): Task? = null

    override suspend fun saveTask(updated: Task) = Unit

    override fun finishSaving(updated: Task?, failed: Boolean) = Unit
  }
}
