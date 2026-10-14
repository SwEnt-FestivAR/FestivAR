// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

class FestivARTestRunner : AndroidJUnitRunner() {
  override fun onCreate(arguments: Bundle?) {
    FirebaseEmulatorSetup.configure()
    super.onCreate(arguments)
  }
}
