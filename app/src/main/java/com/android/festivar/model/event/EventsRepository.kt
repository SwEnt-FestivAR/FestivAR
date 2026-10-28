// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.user.User

/** Represents a repository that manages [Event] items. */
interface EventsRepository {

  /** Generates and returns a new unique identifier for an [Event] item. */
  fun getNewUid(): String

  /**
   * Retrieves all [Event] items from the repository.
   *
   * @return A list of all [Event] items.
   */
  suspend fun getAllEvents(): List<Event>

  /**
   * Retrieves a specific [Event] item by its unique identifier.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param eventId The unique identifier of the [Event] item to retrieve.
   * @return The [Event] item with the specified identifier.
   * @throws NoSuchElementException if the [Event] item is not found.
   */
  suspend fun getEvent(eventId: String): Event

  /**
   * Retrieves the [Event] items the given [User] is a member of.
   *
   * @param userId The unique identifier of the [User].
   * @return The list of [Event] items whose members contain this [User].
   */
  suspend fun getEventsForUser(userId: String): List<Event>

  /**
   * Retrieves the [Event] items whose join code is [code], as [joinCodeOf] spells it: uppercase
   * letters and digits. Usually one item; several when event identifiers share their first six
   * letters and digits, and none when no event carries the code. A stored event that cannot be read
   * is left out instead of failing the lookup.
   *
   * Nothing is thrown for a stored event that cannot be read. Implementations backed by a remote
   * storage may throw exceptions when the storage cannot be reached.
   *
   * @param code The join code an organizer shared.
   * @return The list of [Event] items with this join code, possibly empty.
   */
  suspend fun getEventsByJoinCode(code: String): List<Event>

  /**
   * Adds a new [Event] item to the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param event The [Event] item to add. Its [Event.eventId] must not be used by another [Event]
   *   item.
   * @throws IllegalArgumentException if an [Event] item with the same identifier already exists.
   */
  suspend fun addEvent(event: Event)

  /**
   * Edits an existing [Event] item in the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param eventId The unique identifier of the [Event] item to edit.
   * @param newValue The new value for the [Event] item. Its [Event.eventId] must be equal to
   *   [eventId].
   * @throws IllegalArgumentException if [newValue] does not have the same identifier as [eventId].
   * @throws NoSuchElementException if the [Event] item is not found.
   */
  suspend fun editEvent(eventId: String, newValue: Event)

  /**
   * Deletes an [Event] item from the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param eventId The unique identifier of the [Event] item to delete.
   * @throws NoSuchElementException if the [Event] item is not found.
   */
  suspend fun deleteEvent(eventId: String)

  /**
   * Adds a new [Announcement] to an existing [Event] item of the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param eventId The unique identifier of the [Event] item to add [announcement] to.
   * @param announcement The [Announcement] to add. Its [Announcement.announcementId] must not be
   *   used by another [Announcement] of the [Event] item.
   * @throws IllegalArgumentException if the [Event] item already has an [Announcement] with the
   *   same identifier.
   * @throws NoSuchElementException if the [Event] item is not found.
   */
  suspend fun addAnnouncement(eventId: String, announcement: Announcement)

  /**
   * Edits an existing [Announcement] of an [Event] item of the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param eventId The unique identifier of the [Event] item the [Announcement] belongs to.
   * @param announcement The new value for the [Announcement], replacing the one with the same
   *   [Announcement.announcementId].
   * @throws IllegalArgumentException if the [Event] item has no [Announcement] with the same
   *   identifier as [announcement].
   * @throws NoSuchElementException if the [Event] item is not found.
   */
  suspend fun editAnnouncement(eventId: String, announcement: Announcement)

  /**
   * Deletes an [Announcement] from an [Event] item of the repository.
   *
   * Besides the exceptions documented below, implementations backed by a remote storage may throw
   * other exceptions when the storage cannot be reached or holds invalid data.
   *
   * @param eventId The unique identifier of the [Event] item the [Announcement] belongs to.
   * @param announcementId The unique identifier of the [Announcement] to delete.
   * @throws IllegalArgumentException if the [Event] item has no [Announcement] with identifier
   *   [announcementId].
   * @throws NoSuchElementException if the [Event] item is not found.
   */
  suspend fun deleteAnnouncement(eventId: String, announcementId: String)
}
