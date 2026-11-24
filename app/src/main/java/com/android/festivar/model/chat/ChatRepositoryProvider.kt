// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/** Provides the chat repository used by the application. */
object ChatRepositoryProvider {
  private val firebaseRepository: ChatRepository by lazy {
    ChatRepositoryFirebase(Firebase.firestore)
  }

  @Volatile private var override: ChatRepository? = null

  var repository: ChatRepository
    get() = override ?: firebaseRepository
    set(value) {
      override = value
    }

  fun reset() {
    override = null
  }
}
