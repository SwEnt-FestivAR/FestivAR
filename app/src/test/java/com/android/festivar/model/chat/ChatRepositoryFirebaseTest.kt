// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.chat

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import java.lang.reflect.InvocationTargetException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChatRepositoryFirebaseTest {
  /** Breaks if documentToChat accepts a Firestore snapshot that does not exist. */
  @Test
  fun documentToChat_rejectsNonexistentDocument() {
    val chatId = "missing-chat"
    val document =
        mockk<DocumentSnapshot> {
          every { exists() } returns false
          every { id } returns chatId
        }
    val repository = ChatRepositoryFirebase(mockk<FirebaseFirestore>())
    val converter =
        ChatRepositoryFirebase::class
            .java
            .getDeclaredMethod("documentToChat", DocumentSnapshot::class.java)
            .apply { isAccessible = true }

    val invocationError =
        assertThrows(InvocationTargetException::class.java) {
          converter.invoke(repository, document)
        }

    assertEquals(IllegalStateException::class.java, invocationError.cause!!::class.java)
    assertEquals("Chat '$chatId' does not exist.", invocationError.cause!!.message)
  }
}
