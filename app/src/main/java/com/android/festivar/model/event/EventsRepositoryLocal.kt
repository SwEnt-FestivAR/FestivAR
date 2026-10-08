// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import java.util.UUID

/**
 * Represents a repository that manages a local list of events.
 *
 * @param initialEvents The [Event] items the repository starts with. Their identifiers must be
 *   unique.
 */
class EventsRepositoryLocal(initialEvents: List<Event> = emptyList()) : EventsRepository {
  private val events: MutableList<Event> = initialEvents.toMutableList()

  init {
    require(events.map { it.eventId }.distinct().size == events.size) {
      "EventsRepositoryLocal: initialEvents contains two Events with the same eventId"
    }
  }

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllEvents(): List<Event> {
    return events.toList()
  }

  override suspend fun getEvent(eventId: String): Event {
    return events.find { it.eventId == eventId }
        ?: throw NoSuchElementException("EventsRepositoryLocal: Event $eventId not found")
  }

  override suspend fun getEventsForUser(userId: String): List<Event> {
    return events.filter { event -> event.members.any { it.uid == userId } }
  }

  override suspend fun addEvent(event: Event) {
    require(events.none { it.eventId == event.eventId }) {
      "EventsRepositoryLocal: an Event with eventId ${event.eventId} already exists"
    }
    events.add(event)
  }

  override suspend fun editEvent(eventId: String, newValue: Event) {
    require(newValue.eventId == eventId) {
      "EventsRepositoryLocal: newValue.eventId (${newValue.eventId}) must be equal to eventId " +
          "($eventId)"
    }

    val index = events.indexOfFirst { it.eventId == eventId }

    if (index != -1) {
      events[index] = newValue
    } else {
      throw NoSuchElementException("EventsRepositoryLocal: Event $eventId not found")
    }
  }

  override suspend fun deleteEvent(eventId: String) {
    val index = events.indexOfFirst { it.eventId == eventId }
    if (index != -1) {
      events.removeAt(index)
    } else {
      throw NoSuchElementException("EventsRepositoryLocal: Event $eventId not found")
    }
  }
}
