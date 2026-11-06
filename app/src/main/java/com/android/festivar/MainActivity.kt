// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.datarepository.DataRepositoryFirebase
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.announcement.announcementGraph
import com.android.festivar.navigation.authentication.authGraph
import com.android.festivar.navigation.event.eventGraph
import com.android.festivar.navigation.profile.profileGraph
import com.android.festivar.navigation.task.taskGraph
import com.android.festivar.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    // The system start screen is the app's only splash, shown while the app starts
    installSplashScreen()
    super.onCreate(savedInstanceState)
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
 */
@Composable
fun FestivAR(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    dataRepository: DataRepository = remember { DataRepositoryFirebase() },
    startDestinationOverride: String? = null,
) {
  val navController = rememberNavController()

  val startDestination =
      startDestinationOverride
          ?: if (dataRepository.authRepository.currentUser != null) AppScreens.EventsOverview.name
          else AppScreens.Login.name

  NavHost(
      navController = navController,
      startDestination = startDestination,
  ) {
    authGraph(navController, credentialManager, dataRepository.authRepository)
    eventGraph(navController, dataRepository.authRepository)
    taskGraph(navController, dataRepository.authRepository)
    announcementGraph(navController, dataRepository.authRepository)
    profileGraph(navController, dataRepository.authRepository)
  }
}

@Composable
fun ScreenName(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
