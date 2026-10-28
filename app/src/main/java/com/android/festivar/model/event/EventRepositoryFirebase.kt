// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import android.util.Log
import com.android.festivar.model.task.TaskRepositoryFirebase
import com.android.festivar.model.user.UserRepositoryFirebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val EVENT_COLLECTION_PATH = "events"
private const val TAG = "EventRepositoryFirebase"

class EventRepositoryFirebase(private val db: FirebaseFirestore) : EventsRepository {

  private val eventCollection
    get() = db.collection(EVENT_COLLECTION_PATH)

  private val taskRepository by lazy { TaskRepositoryFirebase(db) }
  private val userRepo by lazy { UserRepositoryFirebase(db) }

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

  /**
   * Firestore stores the code on each document, so a change to [joinCodeOf] or [JOIN_CODE_LENGTH]
   * makes every stored code stale and needs a backfill of the existing events.
   */
  override suspend fun getEventsByJoinCode(code: String): List<Event> {
    // A malformed document yields no event instead of failing the whole lookup. Cancellation is
    // rethrown, so a lookup canceled while it reads members or tasks never returns a partial list.
    return eventCollection.whereEqualTo("joinCode", code).get().await().documents.mapNotNull {
      try {
        documentToEvent(it)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Skipping event ${it.id} that could not be read", e)
        null
      }
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
        // Stored so a join can be a single query instead of a download of the whole collection
        "joinCode" to joinCodeOf(event),
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

    val members =
        (document.get("members") as? List<*>).orEmpty().map { value ->
          val userId =
              value as? String ?: throw IllegalArgumentException("Invalid member data: $value")
          userRepo.getUser(userId)
        }

    return Event(
        eventId = document.getString("eventId") ?: document.id,
        title =
            document.getString("title")
                ?: throw IllegalArgumentException("Event '${document.id}' has missing title"),
        description = document.getString("description") ?: "",
        members = members,
        startDate = ZonedDateTime.parse(document.getString("startDate")),
        endDate = ZonedDateTime.parse(document.getString("endDate")),
        location = document.getString("location") ?: "",
        tasks = tasks,
        closed = document.getBoolean("closed") ?: false,
    )
  }
}
