package com.android.festivar.model.event

import com.android.festivar.model.task.Task
import com.android.festivar.model.temporary.User
import java.time.LocalDate

/**
 * Represents an [Event] in the FestivAR application. an [Event] os the core element of the
 * application as it is its purpose. An [Event] can be any organized meeting necessitating
 * people organization as well as a clear overview of it, it can be a festival, a music concert,
 * a wedding etc.
 *
 * An [Event] is immutable: its fields can never be changed. Every "modification" ([copy],
 * [addMember], [removeMember], [close],...) returns a new [Event] instead, so the result must be
 * kept as: `event = event.close()`. And for any other change of one of the [Event] variables, it
 * has to be done through a copy of this [Event]. This implementation is the consequence of working
 * with Compose that needs, to refresh the View automatically, to have an object that is reassigned.
 * Only changing a field of an object would not automatically refresh the View.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [title] is not blank;
 * - [eventId] is not blank;
 * - a [User] is a joined member at most once (no duplicates);
 * - a [Task] is unique in the event (no duplicates);
 * - [endDate] is not before [startDate];
 *
 * @property eventId Unique identifier of the [Event].
 * @property title Name of the [Event], shown as the screen title.
 * @property description Free-text description of specifications about the [Event].
 * @property members People who joined or were assigned the [Event].
 * @property startDate Starting date of the [Event], or `null` if not set.
 * @property endDate Ending date of the [Event], or `null` if not set.
 * @property location Where the [Event] takes place.
 * @property tasks Tasks that are related, that belong to the [Event].
 * @property closed Whether the [Event] is still ongoing or not.
 */
data class Event(
    val eventId: String,
    val title: String,
    val description: String = "",
    val members: List<User> = emptyList(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val location: String = "",
    val tasks: List<Task> = emptyList(),
    val closed: Boolean = false
){

    init {
        require(eventId.isNotBlank()) { "The eventId cannot be empty." }
        require(title.isNotBlank()) { "The title cannot be empty." }
        require(tasks.distinct().size == tasks.size) { "A task is duplicated." }
        require(members.distinct().size == members.size) { "A user is registered twice." }
        require(startDate == null || endDate == null || !endDate.isBefore(startDate)) {
            "endDate must not be before startDate."
        }
    }

    /**
     * Meant to inform that the [Event] is finished/closed by returning a closed version
     * copy of `this`.
     *
     * @return The [Event] but finished/closed.
     */
    fun close(): Event {
        return copy(closed = true)
    }

    /**
     * Adds a new [User] to the [Event] by returning a version of `this` with [user] added to
     * the [members] list.
     *
     * @param user The [User] that is going to join `this`, to be added to [members] list.
     * @return The [Event] but with [user] added to it.
     * @throws IllegalArgumentException If [user] is already in the event.
     */
    fun addMember(user: User): Event {
        require(user !in members) { "This user is already registered in the event." }
        return copy(members = members + user)
    }

    /**
     * Removes an assigned [User] from the [Task] by returning a version of `this` with [user] removed
     * from the [members] list.
     *
     * @param user The [User] that is going to be removed from `this`, from [members] list.
     * @return The [Event] but with [user] removed from it.
     * @throws IllegalArgumentException If [user] is not assigned to `this`.
     */
    fun removeMember(user: User): Event {
        require(user in members) { "This user is not a member of the event." }

        return copy(members = members - user)
    }

    /**
     * Adds a new [Task] to the [Event] by returning a version of `this` with [task] added to
     * the [tasks] list.
     *
     * @param task The [Task] that is going to be added to `this`, to be added to [tasks] list.
     * @return The [Event] but with [task] added to it.
     * @throws IllegalArgumentException If [task] is already in the event.
     */
    fun addTask(task: Task): Event {
        require(task !in tasks) { "This task is already in the event." }
        return copy(tasks = tasks + task)
    }

    /**
     * Removes a [Task] from the [Event] by returning a version of `this` with [task] removed
     * from the [tasks] list.
     *
     * @param task The [Task] that is going to be removed from `this`, from [tasks] list.
     * @return The [Event] but with [task] removed from it.
     * @throws IllegalArgumentException If [task] is not assigned to `this`.
     */
    fun removeTask(task: Task): Event {
        require(task in tasks) { "This task is not in the event." }

        return copy(tasks = tasks - task)
    }

}

