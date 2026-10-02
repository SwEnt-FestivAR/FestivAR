package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class AuthRepositoryFirebase(
    private val googleSignInHelper: GoogleSignInHelper = GoogleSignInHelper()
) : AuthRepository {
    val currentUser: FirebaseUser?
        get() = Firebase.auth.currentUser

    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
        runCatching {
            val firebaseCredential = googleSignInHelper.getFirebaseCredential(credential)
            Firebase.auth.signInWithCredential(firebaseCredential).await().user
                ?: throw IllegalStateException("Firebase sign-in returned no user")
        }

    override suspend fun signOut(): Result<Unit> = runCatching { Firebase.auth.signOut() }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return runCatching {
            Firebase.auth.signInWithEmailAndPassword(email, password).await().user
                ?: throw IllegalStateException("Firebase sign-in returned no user")
        }
    }
}
