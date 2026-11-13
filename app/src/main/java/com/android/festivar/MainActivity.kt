// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.festivar.model.audio.AndroidVoicePlayer
import com.android.festivar.model.audio.AndroidVoiceRecorder
import com.android.festivar.model.audio.AudioStorageFirebase
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.datarepository.DataRepositoryFirebase
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.BottomBarNavigator
import com.android.festivar.navigation.announcement.announcementGraph
import com.android.festivar.navigation.authentication.authGraph
import com.android.festivar.navigation.bottomBarStateFor
import com.android.festivar.navigation.event.eventGraph
import com.android.festivar.navigation.profile.profileGraph
import com.android.festivar.navigation.task.taskGraph
import com.android.festivar.ui.components.BottomNavigationBar
import com.android.festivar.ui.settings.applySavedSettings
import com.android.festivar.ui.task.details.VoiceNoteServices
import com.android.festivar.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    // The system start screen is the app's only splash, shown while the app starts
    installSplashScreen()
    super.onCreate(savedInstanceState)
    applySavedSettings(this)
    // Source - https://stackoverflow.com/q/73598309
    // Posted by Alix Blaine
    // Retrieved 2026-11-05, License - CC BY-SA 4.0

    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    setContent { AppTheme { Surface(modifier = Modifier.fillMaxSize()) { FestivAR() } } }
  }
}

/**
 * `FestivAR` is the main composable function that sets up the whole app UI. It initializes the
 * navigation controller and defines the navigation graph.
 *
 * @param voiceNoteServices creates what records, keeps and plays the voice note of a task: the
 *   microphone and the speaker of the device, and Firebase Storage.
 *
 * The bottom bar shows under the top-level screens and is hidden on forms and authentication. Its
 * selected tab is derived from the current back stack entry: a task's details keep the tab they
 * were opened from, carried in their route (see [bottomBarStateFor]).
 */
@Composable
fun FestivAR(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    dataRepository: DataRepository = remember { DataRepositoryFirebase() },
    startDestinationOverride: String? = null,
    voiceNoteServices: () -> VoiceNoteServices? = {
      VoiceNoteServices(
          recorder = AndroidVoiceRecorder(context),
          storage = AudioStorageFirebase(),
          player = AndroidVoicePlayer(context),
      )
    },
) {
  val navController = rememberNavController()

  val startDestination =
      startDestinationOverride
          ?: if (dataRepository.authRepository.currentUser != null) AppScreens.EventsOverview.name
          else AppScreens.Login.name

  val entry by navController.currentBackStackEntryAsState()
  val bar = bottomBarStateFor(entry?.destination?.route, entry?.arguments?.getString("from"))
  val bottomBarNavigator = remember(navController) { BottomBarNavigator(navController) }

  Scaffold(
      // The screens' own Scaffolds handle the system bars; a second inset here would double the
      // top padding.
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        bar?.let {
          BottomNavigationBar(
              selectedTab = it.selectedTab,
              onTabSelected = { tab -> bottomBarNavigator.onTabSelected(tab, it) },
              subScreen = it.subScreen,
          )
        }
      },
  ) { padding ->
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(padding),
    ) {
      authGraph(navController, credentialManager, dataRepository)
      eventGraph(navController, dataRepository)
      taskGraph(navController, dataRepository, voiceNoteServices)
      announcementGraph(navController, dataRepository)
      profileGraph(navController, dataRepository)
    }
  }
}

@Composable
fun ScreenName(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
