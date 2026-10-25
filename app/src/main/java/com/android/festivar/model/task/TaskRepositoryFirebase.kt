// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.task

import com.android.festivar.model.user.UserRepositoryFirebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.collections.map
import kotlinx.coroutines.tasks.await

const val TASK_COLLECTION_PATH = "tasks"

class TaskRepositoryFirebase(
    private val db: FirebaseFirestore,
) : TasksRepository {

  private val userRepo by lazy { UserRepositoryFirebase(db) }

  private val taskCollection
    get() = db.collection(TASK_COLLECTION_PATH)

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllTasks(eventId: String): List<Task> {
    return taskCollection.whereEqualTo("eventId", eventId).get().await().documents.map {
      documentToTask(it)
    }
  }

  override suspend fun getTask(taskId: String): Task {
    val document = taskCollection.document(taskId).get().await()
    if (!document.exists()) {
      throw NoSuchElementException("Task '$taskId' does not exist.")
    }
    return documentToTask(document)
  }

  override suspend fun addTask(task: Task) {
    val document = taskCollection.document(task.taskId)
    db.runTransaction { transaction ->
          require(!transaction.get(document).exists()) {
            "A Task with taskId '${task.taskId}' already exists."
          }
          transaction.set(document, toFirestoreData(task))
        }
        .await()
  }

  override suspend fun editTask(
      taskId: String,
      newValue: Task,
  ) {
    require(taskId == newValue.taskId) { "The edited Task must keep its existing taskId." }
    val document = taskCollection.document(taskId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Task '$taskId' does not exist.")
          }

          transaction.update(document, toFirestoreData(newValue))
        }
        .await()
  }

  override suspend fun deleteTask(taskId: String) {
    val document = taskCollection.document(taskId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Task '$taskId' does not exist.")
          }

          transaction.delete(document)
        }
        .await()
  }

  private fun toFirestoreData(task: Task): Map<String, Any?> {
    return mapOf(
        "taskId" to task.taskId,
        "eventId" to task.eventId,
        "title" to task.title,
        "description" to task.description,
        "startTime" to task.startTime?.toString(),
        "endTime" to task.endTime?.toString(),
        "estimatedTime" to task.estimatedTime?.toNanos(),
        "location" to task.location,
        "priority" to task.priority.name,
        "maxAssign" to task.maxAssign,
        "assignees" to task.assignees.map { user -> user.uid },
        "completed" to task.completed,
        "ownerId" to task.ownerId,
    )
  }

  private suspend fun documentToTask(document: DocumentSnapshot): Task {
    check(document.exists()) { "Task '${document.id}' does not exist." }

    val ass =
        (document.get("assignees") as? List<*>).orEmpty().map { value ->
          val userId =
              value as? String ?: throw IllegalArgumentException("Invalid member data: $value")
          userRepo.getUser(userId)
        }

    return Task(
        taskId = document.getString("taskId") ?: document.id,
        eventId = document.getString("eventId").required("eventId", document.id),
        title = document.getString("title").required("title", document.id),
        description = document.getString("description") ?: "",
        startTime = document.getString("startTime")?.let(ZonedDateTime::parse),
        endTime = document.getString("endTime")?.let(ZonedDateTime::parse),
        estimatedTime = document.getLong("estimatedTime")?.let(Duration::ofNanos),
        location = document.getString("location") ?: "",
        priority = document.getString("priority")?.let(Priority::valueOf) ?: Priority.NONE,
        maxAssign = document.getLong("maxAssign")?.toInt() ?: 1,
        assignees = ass,
        completed = document.getBoolean("completed") ?: false,
        // Tasks saved before owners existed have no ownerId. They are deleted from Firestore, so a
        // missing one is an error, not a case to handle.
        ownerId = document.getString("ownerId").required("ownerId", document.id),
    )
  }

  private fun String?.required(field: String, documentId: String): String {
    return this ?: error("Task '$documentId' has missing $field")
  }
}
