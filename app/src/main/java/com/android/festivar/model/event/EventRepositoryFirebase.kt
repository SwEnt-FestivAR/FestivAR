package com.android.festivar.model.event

import com.android.festivar.model.task.TaskRepositoryFirebase
import com.android.festivar.model.user.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.tasks.await

private const val EVENT_COLLECTION_PATH = "events"

class EventRepositoryFirebase(private val db: FirebaseFirestore) : EventsRepository {

  private val eventCollection
    get() = db.collection(EVENT_COLLECTION_PATH)

  private val taskRepository by lazy { TaskRepositoryFirebase(db) }

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllEvents(): List<Event> {
    return eventCollection.get().await().documents.map { documentToEvent(it) }
  }

  override suspend fun getEvent(eventId: String): Event {
    val document = eventCollection.document(eventId).get().await()
    if (!document.exists()) {
      throw NoSuchElementException("Event '$eventId' does not exist.")
    }
    return documentToEvent(document)
  }

  override suspend fun getEventsForUser(userId: String): List<Event> {
    return eventCollection.whereArrayContains("members", userId).get().await().documents.map {
      documentToEvent(it)
    }
  }

  override suspend fun addEvent(event: Event) {
    val document = eventCollection.document(event.eventId)
    db.runTransaction { transaction ->
          require(!transaction.get(document).exists()) {
            "An Event with eventId '${event.eventId}' already exists."
          }
          transaction.set(document, toFirestoreData(event))
        }
        .await()
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    require(eventId == newValue.eventId) { "The edited Event must keep its existing eventId" }
    val document = eventCollection.document(eventId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Event '$eventId' does not exist.")
          }
          transaction.update(document, toFirestoreData(newValue))
        }
        .await()
  }

  override suspend fun deleteEvent(eventId: String) {
    val document = eventCollection.document(eventId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Event '$eventId' does not exist.")
          }
          transaction.delete(document)
        }
        .await()
  }

  private fun toFirestoreData(event: Event): Map<String, Any?> {
    return mapOf(
        "eventId" to event.eventId,
        "title" to event.title,
        "description" to event.description,
        "members" to event.members.map { it.uid },
        "startDate" to event.startDate.toString(),
        "endDate" to event.endDate.toString(),
        "location" to event.location,
        "tasks" to event.tasks.map { it.taskId },
        "closed" to event.closed,
    )
  }

  private suspend fun documentToEvent(document: DocumentSnapshot): Event {
    check(document.exists()) { "Event '${document.id}' does not exist." }

    val tasks =
        (document.get("tasks") as? List<*>).orEmpty().map { value ->
          val taskId =
              value as? String ?: throw IllegalArgumentException("Invalid task data: $value")
          taskRepository.getTask(taskId)
        }

    return Event(
        eventId = document.getString("eventId") ?: document.id,
        title =
            document.getString("title")
                ?: throw IllegalArgumentException("Event '${document.id}' has missing title"),
        description = document.getString("description") ?: "",
        members =
            (document.get("members") as? List<*>).orEmpty().map { value ->
              when (value) {
                is String -> User(value, "", "")
                is Map<*, *> ->
                    User(
                        uid = value["uid"] as? String ?: "",
                        name = value["name"] as? String ?: "",
                        surname = value["surname"] as? String ?: "",
                    )
                else -> throw IllegalArgumentException("Invalid member data: $value")
              }
            },
        startDate = ZonedDateTime.parse(document.getString("startDate")),
        endDate = ZonedDateTime.parse(document.getString("endDate")),
        location = document.getString("location") ?: "",
        tasks = tasks,
        closed = document.getBoolean("closed") ?: false,
    )
  }
}
