package com.android.festivar

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.resources.C
import com.android.festivar.ui.task.create.CreateTaskScreen
import com.android.festivar.ui.task.create.CreateTaskViewModel
import com.android.festivar.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        // A surface container using the FestivAR background color from the theme
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTag = C.Tag.main_screen_container },
            color = MaterialTheme.colorScheme.background,
        ) {
          // TEMPORARY, local testing only: do not push.
          CreateTaskTestHost(onClose = { finish() })
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier.semantics { testTag = C.Tag.greeting })
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  AppTheme { Greeting("Android") }
}

/** TEMPORARY host to try the Create task screen by hand. Do not push. */
@Composable
private fun CreateTaskTestHost(onClose: () -> Unit) {
  val repository = remember { TasksRepositoryLocal() }
  val viewModel: CreateTaskViewModel =
      viewModel(factory = CreateTaskViewModel.factory("test-event", repository))
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  LaunchedEffect(viewModel) {
    viewModel.uiState.collect { Log.d("CreateTaskTest", "State title='${it.title}'") }
  }
  CreateTaskScreen(
      viewModel = viewModel,
      onBack = onClose,
      onTaskCreated = {
        scope.launch {
          Log.d("CreateTaskTest", "Saved tasks: ${repository.getAllTasks()}")
          Toast.makeText(context, "Task created (see Logcat: CreateTaskTest)", Toast.LENGTH_LONG)
              .show()
        }
      },
  )
}
