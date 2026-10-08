// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.task

import com.android.festivar.model.temporary.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val TASK_COLLECTION_PATH = "tasks"

class TaskRepositoryFirebase(
    private val db: FirebaseFirestore,
) : TasksRepository {
  private val taskCollection
    get() = db.collection(TASK_COLLECTION_PATH)

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllTasks(eventId: String): List<Task> {
    return taskCollection
        .whereEqualTo("eventId", eventId)
        .get()
        .await()
        .documents
        .map(::documentToTask)
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
    taskCollection.document(taskId).update(toFirestoreData(newValue)).await()
  }

  override suspend fun deleteTask(taskId: String) {
    val document = taskCollection.document(taskId)
    check(document.get().await().exists()) { "Task '$taskId' does not exist." }
    document.delete().await()
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
        "assignees" to task.assignees.map(User::uid),
        "completed" to task.completed,
    )
  }

  private fun documentToTask(document: DocumentSnapshot): Task {
    check(document.exists()) { "Task '${document.id}' does not exist." }

    return Task(
        taskId = document.getString("taskId") ?: document.id,
        eventId = document.getString("eventId").required("eventId", document.id),
        title = document.getString("title").required("title", document.id),
        description = document.getString("description") ?: "",
        startTime = document.getString("startTime")?.let(LocalDateTime::parse),
        endTime = document.getString("endTime")?.let(LocalDateTime::parse),
        estimatedTime = document.getLong("estimatedTime")?.let(Duration::ofNanos),
        location = document.getString("location") ?: "",
        priority = document.getString("priority")?.let(Priority::valueOf) ?: Priority.NONE,
        maxAssign = document.getLong("maxAssign")?.toInt() ?: 1,
        assignees =
            (document.get("assignees") as? List<*>).orEmpty().map { value ->
              when (value) {
                // When the assignee is stored as a string (the UID), we create a User with that
                // UID.
                is String -> User(value)
                // When the assignee is stored as a map (with a "uid" field), we create a User with
                // that UID.
                is Map<*, *> -> User(value["uid"] as? String ?: error("Invalid assignee"))
                else -> error("Invalid assignee")
              }
            },
        completed = document.getBoolean("completed") ?: false,
    )
  }

  private fun String?.required(field: String, documentId: String): String {
    return this ?: error("Task '$documentId' has missing $field")
  }
}
