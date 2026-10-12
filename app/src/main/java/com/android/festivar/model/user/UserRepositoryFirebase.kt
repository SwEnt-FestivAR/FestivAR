// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val USER_COLLECTION_PATH = "users"
private const val FIRESTORE_IN_QUERY_LIMIT = 30

class UserRepositoryFirebase(
    private val db: FirebaseFirestore,
) : UsersRepository {
  private val userCollection
    get() = db.collection(USER_COLLECTION_PATH)

  override fun getNewUid(): String {
    return userCollection.document().id
  }

  override suspend fun getAllUsers(): List<User> {
    return userCollection.get().await().documents.map(::documentToUser)
  }

  override suspend fun getUser(uId: String): User {
    val document = userCollection.document(uId).get().await()
    if (!document.exists()) {
      throw NoSuchElementException("User '$uId' does not exist.")
    }
    return documentToUser(document)
  }

  override suspend fun getListUsers(uIds: List<String>): List<User> {
    if (uIds.isEmpty()) return emptyList()

    val usersByUid =
        uIds
            .distinct()
            .chunked(FIRESTORE_IN_QUERY_LIMIT)
            .flatMap { uidChunk ->
              userCollection
                  .whereIn(FieldPath.documentId(), uidChunk)
                  .get()
                  .await()
                  .documents
                  .map(::documentToUser)
            }
            .associateBy(User::uid)

    return uIds.map { uid ->
      usersByUid[uid] ?: throw NoSuchElementException("User '$uid' does not exist.")
    }
  }

  override suspend fun addUser(user: User) {
    val document = userCollection.document(user.uid)
    db.runTransaction { transaction ->
          require(!transaction.get(document).exists()) {
            "A User with uid '${user.uid}' already exists."
          }
          transaction.set(document, toFirestoreData(user))
        }
        .await()
  }

  override suspend fun editUser(uId: String, newValue: User) {
    require(uId == newValue.uid) { "The edited User must keep its existing uid." }
    val document = userCollection.document(uId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("User '$uId' does not exist.")
          }
          transaction.update(document, toFirestoreData(newValue))
        }
        .await()
  }

  override suspend fun deleteUser(uId: String) {
    val document = userCollection.document(uId)
    if (!document.get().await().exists()) {
      throw NoSuchElementException("User '$uId' does not exist.")
    }
    document.delete().await()
  }

  private fun toFirestoreData(user: User): Map<String, Any?> {
    return mapOf(
        "name" to user.name,
        "surname" to user.surname,
        "email" to user.email,
        "joinedEvents" to user.joinedEvents,
        "tasksAssigned" to user.tasksAssigned,
    )
  }

  private fun documentToUser(document: DocumentSnapshot): User {
    check(document.exists()) { "User '${document.id}' does not exist." }
    return User(
        uid = document.id,
        name = document.getString("name") ?: "",
        surname = document.getString("surname") ?: "",
        email = document.getString("email") ?: "",
        joinedEvents = document.stringList("joinedEvents"),
        tasksAssigned = document.stringList("tasksAssigned"),
    )
  }

  private fun DocumentSnapshot.stringList(field: String): List<String> {
    return (get(field) as? List<*>).orEmpty().map { value ->
      value as? String ?: error("User '$id' has invalid $field.")
    }
  }
}
