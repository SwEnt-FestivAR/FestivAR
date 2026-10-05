package com.android.festivar.model.event

import com.android.festivar.model.temporary.User

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
     * @param eventId The unique identifier of the [Event] item to retrieve.
     *
     * @return The [Event] item with the specified identifier.
     *
     * @throws NoSuchElementException if the [Event] item is not found.
     */
    suspend fun getEvent(eventId: String): Event

    /**
     * Retrieves the [Event] items the given [User] is a member of.
     *
     * @param userId The unique identifier of the [User].
     *
     * @return The list of [Event] items whose members contain this [User].
     */
    suspend fun getEventsForUser(userId: String): List<Event>

    /**
     * Adds a new [Event] item to the repository.
     *
     * @param event The [Event] item to add. Its [Event.eventId] must not be used by another [Event] item.
     * 
     * @throws IllegalArgumentException if an [Event] item with the same identifier already exists.
     */
    suspend fun addEvent(event: Event)

    /**
     * Edits an existing [Event] item in the repository.
     *
     * @param eventId The unique identifier of the [Event] item to edit.
     * @param newValue The new value for the [Event] item. Its [Event.eventId] must be equal to [eventId].
     *
     * @throws IllegalArgumentException if [newValue] does not have the same identifier as [eventId].
     * @throws NoSuchElementException if the [Event] item is not found.
     */
    suspend fun editEvent(eventId: String, newValue: Event)

    /**
     * Deletes an [Event] item from the repository.
     *
     * @param eventId The unique identifier of the [Event] item to delete.
     * @throws NoSuchElementException if the [Event] item is not found.
     */
    suspend fun deleteEvent(eventId: String)
}
