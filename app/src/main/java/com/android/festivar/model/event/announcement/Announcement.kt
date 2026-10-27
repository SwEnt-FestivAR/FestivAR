package com.android.festivar.model.event.announcement

import com.android.festivar.model.task.Priority
import java.time.ZonedDateTime

/**
 * Represents an [Announcement] in the FestivAR application. An [Announcement] is something, in the context of an
 * [com.android.festivar.model.event.Event], that provides information that needs to be shared to the members of the [com.android.festivar.model.event.Event].
 *
 * An [Announcement] is immutable: its fields can never be changed. Every "modification" returns a new
 * [Announcement] instead, so the result must be kept as: `announcement = announcement.{function}`.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [title] is not blank;
 * - [authorId] is not blank;
 * - [endTime], if not null, is strictly after [createdAt].
 *
 * @property announcementId Unique identifier of the [Announcement].
 * @property title Name/subject visible of the [Announcement].
 * @property authorId Unique identifier of the user author of the [Announcement].
 * @property description Free-text description, content-body of the [Announcement].
 * @property priority How urgent and important the [Announcement] is.
 * @property createdAt Time of the creation of the [Announcement].
 * @property endTime If the [Announcement] may not be pertinent after a certain time, the time
 * after which it is obsolete.
 * @property links Useful links attached to the [Announcement].
 * @property pinned States if the [Announcement] is pinned or not.
 */
data class Announcement(
    val announcementId: String,
    val title: String,
    val authorId: String,
    val description: String = "",
    val priority: Priority = Priority.NONE,
    val createdAt: ZonedDateTime,
    val endTime: ZonedDateTime? = null,
    val links: List<String> = emptyList(),
    val pinned: Boolean = false,
) {

    init {
        require(announcementId.isNotBlank()) { "The announcementId cannot be empty." }
        require(title.isNotBlank()) { "The title cannot be empty." }
        require(authorId.isNotBlank()) { "The authorId cannot be empty." }
        require(endTime == null || createdAt.isBefore(endTime)) {
            "endTime must not be before or equal to createdAt."
        }
        require(links.none { it.isBlank() }) { "A link cannot be empty." }
        require(links.distinct().size == links.size) { "A link is duplicated." }
    }


}