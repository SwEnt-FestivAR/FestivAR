// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSignInHelper {
  fun getFirebaseCredential(credential: Credential): AuthCredential {
    require(
        !(credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
    ) {
      "Credential is not a Google ID token credential"
    }
    val idToken =
        credential.data.getString(
            "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN"
        )
            ?: credential.data.getString("id_token")
            ?: GoogleIdTokenCredential.createFrom(credential.data).idToken
    return GoogleAuthProvider.getCredential(idToken, null)
  }
}
