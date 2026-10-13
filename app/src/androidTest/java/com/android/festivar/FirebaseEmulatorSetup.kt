// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

object FirebaseEmulatorSetup {
  private var configured = false

  @Synchronized
  fun configure() {
    if (configured) return

    Firebase.auth.useEmulator("10.0.2.2", 9099)
    Firebase.firestore.useEmulator("10.0.2.2", 8080)
    configured = true
  }
}
