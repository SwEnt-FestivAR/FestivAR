// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import android.util.Log
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.map.readStoredLocation
import com.android.festivar.model.task.TaskRepositoryFirebase
import com.android.festivar.model.user.UsersRepositoryFirebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val EVENT_COLLECTION_PATH = "events"
private const val TAG = "EventRepositoryFirebase"
private const val ANNOUNCEMENTS_FIELD = "announcements"
private const val ROLES_FIELD = "roles"
private const val LOCATION_FIELD = "location"

class EventRepositoryFirebase(private val db: FirebaseFirestore) : EventsRepository {

  private val eventCollection
    get() = db.collection(EVENT_COLLECTION_PATH)

  private val taskRepository by lazy { TaskRepositoryFirebase(db) }
  private val userRepo by lazy { UsersRepositoryFirebase(db) }

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

  override suspend fun addAnnouncement(eventId: String, announcement: Announcement) {
    updateAnnouncements(eventId) { it.withAnnouncement(announcement) }
  }

  override suspend fun editAnnouncement(eventId: String, announcement: Announcement) {
    updateAnnouncements(eventId) { it.withEditedAnnouncement(announcement) }
  }

  override suspend fun setAnnouncementPinned(
      eventId: String,
      announcementId: String,
      pinned: Boolean,
  ): Announcement {
    val document = eventCollection.document(eventId)
    return db.runTransaction { transaction ->
          val snapshot = transaction.get(document)
          if (!snapshot.exists()) {
            throw NoSuchElementException("Event '$eventId' does not exist.")
          }
          val announcements = readAnnouncements(snapshot)
          val current =
              announcements.announcementOrNull(announcementId)
                  ?: throw IllegalArgumentException("This announcement is not in the event.")
          val updated = current.copy(pinned = pinned)
          transaction.update(
              document,
              ANNOUNCEMENTS_FIELD,
              announcements.withEditedAnnouncement(updated).map { it.toMap() },
          )
          updated
        }
        .await()
  }

  override suspend fun deleteAnnouncement(eventId: String, announcementId: String) {
    updateAnnouncements(eventId) { it.withoutAnnouncement(announcementId) }
  }

  /** In one transaction, so that two concurrent changes cannot overwrite each other. */
  private suspend fun updateAnnouncements(
      eventId: String,
      change: (List<Announcement>) -> List<Announcement>,
  ) {
    val document = eventCollection.document(eventId)
    db.runTransaction { transaction ->
          val snapshot = transaction.get(document)
          if (!snapshot.exists()) {
            throw NoSuchElementException("Event '$eventId' does not exist.")
          }
          val announcements = change(readAnnouncements(snapshot))
          transaction.update(document, ANNOUNCEMENTS_FIELD, announcements.map { it.toMap() })
        }
        .await()
  }

  /**
   * An event saved before announcements were stored has none. Any other non-list value is refused,
   * so that a transaction never overwrites data it could not read.
   */
  private fun readAnnouncements(document: DocumentSnapshot): List<Announcement> {
    val invalid = "Invalid announcement data for event ${document.id}"
    val stored = document.get(ANNOUNCEMENTS_FIELD) ?: return emptyList()
    val list = stored as? List<*> ?: throw IllegalArgumentException("$invalid.")
    return list.map { value ->
      val map = value as? Map<*, *> ?: throw IllegalArgumentException("$invalid.")
      try {
        Announcement.fromMap(map)
      } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("$invalid: ${e.message}", e)
      }
    }
  }

  /**
   * Reads the roles exactly as they are stored. An absent or null field reads as no role at all,
   * while any other invalid value is refused, so that an event is never read with made-up roles.
   *
   * @param document The stored event.
   * @return The stored roles, by user id.
   * @throws IllegalArgumentException If the stored roles are not a map from a user id to the name
   *   of a [Role].
   */
  private fun readRoles(document: DocumentSnapshot): Map<String, Role> {
    val stored = document.get(ROLES_FIELD) ?: return emptyMap()
    val invalid = "Invalid role data for event ${document.id}"
    val map = stored as? Map<*, *> ?: throw IllegalArgumentException("$invalid.")
    return map.entries.associate { (userId, role) ->
      require(userId is String && role is String) { "$invalid: $userId=$role." }
      val storedRole =
          Role.entries.firstOrNull { it.name == role }
              ?: throw IllegalArgumentException("$invalid: unknown role '$role'.")
      userId to storedRole
    }
  }

  /**
   * TEMPORARY, while the database still holds events written before roles existed or by an older
   * version of the app, which changed `members` without `roles`. Repairs the stored roles so that
   * they match the members again, instead of making the event unreadable:
   * - the owner always gets the [Role.OWNER];
   * - a member without a role, or with the [Role.OWNER] without being the owner, gets the
   *   [DEFAULT_ROLE];
   * - the role of a user who is not a member anymore is dropped.
   *
   * To remove it once the database is cleared: delete this function and read the roles with
   * `readRoles(document)` alone in [documentToEvent]. A stored event whose roles do not match its
   * members is then refused by [Event].
   *
   * @param stored The roles as stored, from [readRoles].
   * @param ownerId The [Event.ownerId] of the stored event.
   * @param memberIds The [com.android.festivar.model.user.User.uid] of every member of the event.
   * @return One role for each member, with the [Role.OWNER] for the owner alone.
   */
  private fun repairRoles(
      stored: Map<String, Role>,
      ownerId: String,
      memberIds: List<String>,
  ): Map<String, Role> {
    return memberIds.associateWith { userId ->
      if (userId == ownerId) Role.OWNER
      else stored[userId]?.takeIf { it != Role.OWNER } ?: DEFAULT_ROLE
    }
  }

  private fun toFirestoreData(event: Event): Map<String, Any?> {
    return mapOf(
        "eventId" to event.eventId,
        "ownerId" to event.ownerId,
        "title" to event.title,
        "description" to event.description,
        "members" to event.members.map { it.uid },
        ROLES_FIELD to event.roles.mapValues { it.value.name },
        "startDate" to event.startDate.toString(),
        "endDate" to event.endDate.toString(),
        LOCATION_FIELD to event.location?.toMap(),
        "tasks" to event.tasks.map { it.taskId },
        "closed" to event.closed,
        // Stored so a join can be a single query instead of a download of the whole collection
        "joinCode" to joinCodeOf(event),
        ANNOUNCEMENTS_FIELD to event.announcements.map { it.toMap() },
    )
  }

  private suspend fun documentToEvent(document: DocumentSnapshot): Event {
    check(document.exists()) { "Event '${document.id}' does not exist." }

    val ownerId =
        document.getString("ownerId")
            ?: throw IllegalArgumentException("Event '${document.id}' has no owner")
    val tasks =
        (document.get("tasks") as? List<*>).orEmpty().map { value ->
          val taskId =
              value as? String ?: throw IllegalArgumentException("Invalid task data: $value")
          taskRepository.getTask(taskId)
        }

    val members =
        (document.get("members") as? List<*>)?.map { value ->
          val userId =
              value as? String ?: throw IllegalArgumentException("Invalid member data: $value")
          userRepo.getUser(userId)
        } ?: listOf(userRepo.getUser(ownerId))

    return Event(
        eventId = document.getString("eventId") ?: document.id,
        ownerId = ownerId,
        title =
            document.getString("title")
                ?: throw IllegalArgumentException("Event '${document.id}' has missing title"),
        description = document.getString("description") ?: "",
        members = members,
        // TEMPORARY repair, see repairRoles: `roles = readRoles(document)` once it is removed.
        roles = repairRoles(readRoles(document), ownerId, members.map { it.uid }),
        startDate = ZonedDateTime.parse(document.getString("startDate")),
        endDate = ZonedDateTime.parse(document.getString("endDate")),
        location = readStoredLocation(document.get(LOCATION_FIELD), "event ${document.id}"),
        tasks = tasks,
        closed = document.getBoolean("closed") ?: false,
        announcements = readAnnouncements(document),
    )
  }
}
