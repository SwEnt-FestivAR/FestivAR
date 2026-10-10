// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

// Was forced to test this setup even if the code is trivial
// because of sonar coverage requirements
class FirebaseConfigurationTest {
  private val auth = mockk<FirebaseAuth>(relaxed = true)
  private val firestore = mockk<FirebaseFirestore>(relaxed = true)

  @Test
  fun debugBuildConnectsFirebaseServicesToEmulators() {
    configureFirebase(debug = true, auth = auth, firestore = firestore)

    verify { firestore.useEmulator(EMU_IP, 8080) }
    verify { auth.useEmulator(EMU_IP, 9099) }
  }

  @Test
  fun releaseBuildDoesNotConnectFirebaseServicesToEmulators() {
    configureFirebase(debug = false, auth = auth, firestore = firestore)

    verify(exactly = 0) { firestore.useEmulator(any(), any()) }
    verify(exactly = 0) { auth.useEmulator(any(), any()) }
  }
}
