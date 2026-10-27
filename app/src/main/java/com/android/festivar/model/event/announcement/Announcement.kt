package com.android.festivar.model.event.announcement

import com.android.festivar.model.task.Priority
import java.time.ZonedDateTime

/**
 * Represents an [Announcement] in the FestivAR application. An [Announcement] is something, in the
 * context of an [com.android.festivar.model.event.Event], that provides information that needs to
 * be shared to the members of the [com.android.festivar.model.event.Event].
 *
 * An [Announcement] is immutable: its fields can never be changed. Every "modification" returns a
 * new [Announcement] instead, so the result must be kept as: `announcement =
 * announcement.{function}`.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [announcementId] is not blank;
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
 * @property endTime If the [Announcement] may not be pertinent after a certain time, the time after
 *   which it is obsolete.
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

  /**
   * Returns a new version of `this` but with the [title] changed with the given parameter, so an
   * edited version.
   *
   * @param newTitle The new [title] for the edited [Announcement].
   * @return The [Announcement] but with [title] replaced by [newTitle].
   * @throws IllegalArgumentException If the given [newTitle] is empty.
   */
  fun editTitle(newTitle: String): Announcement {
    require(newTitle.isNotBlank()) { "The title cannot be empty." }
    return copy(title = newTitle)
  }

  /**
   * Returns a new version of `this` but with the [description] changed with the given parameter, so
   * an edited version.
   *
   * @param newDescription The new [description] for the edited [Announcement].
   * @return The [Announcement] but with [description] replaced by [newDescription].
   */
  fun editDescription(newDescription: String): Announcement {
    return copy(description = newDescription)
  }

  /**
   * Returns a new version of `this` but with the [endTime] changed with the given parameter, so an
   * edited version.
   *
   * @param newEndTime The new [endTime] for the edited [Announcement].
   * @return The [Announcement] but with [endTime] replaced by [newEndTime].
   * @throws IllegalArgumentException If the given [newEndTime] is before or equal to [createdAt].
   */
  fun editEndTime(newEndTime: ZonedDateTime?): Announcement {
    require(newEndTime == null || createdAt.isBefore(newEndTime)) {
      "newEndTime must not be before createdAt or equal to it."
    }
    return copy(endTime = newEndTime)
  }

  /**
   * Adds a new link to the [Announcement] by returning a version of `this` with [link] added to the
   * [links] list.
   *
   * @param link The link that is going to be added to `this`, to be added to [links] list.
   * @return The [Announcement] but with [link] added to it.
   * @throws IllegalArgumentException if the given [link] is blank or if it is a duplicate.
   */
  fun addLink(link: String): Announcement {
    require(link.isNotBlank()) { "A link cannot be empty." }
    require(!links.contains(link)) { "The link already exists." }
    return copy(links = links + link)
  }

  /**
   * Removes a link from the [links] by returning a version of `this` with [link] removed from the
   * [links] list.
   *
   * @param link The link that is going to be removed from `this`, from [links] list.
   * @return The [Announcement] but with [link] removed from it.
   * @throws IllegalArgumentException If [link] is not present in the [Announcement].
   */
  fun removeLink(link: String): Announcement {
    require(links.any { it == link }) { "This link is not present in this Announcement." }
    return copy(links = links - link)
  }

  /**
   * Returns a new version of `this` but with the [links] changed with the given parameter, so an
   * edited version.
   *
   * @param newLinks The new [links] for the edited [Announcement].
   * @return The [Announcement] but with [links] replaced by [newLinks].
   */
  fun editLinks(newLinks: List<String>): Announcement {
    require(newLinks.none { it.isBlank() }) { "A link cannot be empty." }
    require(newLinks.distinct().size == newLinks.size) { "A link is duplicated." }
    return copy(links = newLinks)
  }

  /**
   * States if the [Announcement] is obsolete, is still pertinent or not.
   *
   * @param actualTime The actual time when the user reads the [Announcement].
   * @return True only if the [endTime] is not [null] and that the [actualTime] is equal or bigger
   *   than the [endTime].
   */
  fun isObsolete(actualTime: ZonedDateTime): Boolean {
    return endTime != null && !actualTime.isBefore(endTime)
  }

  /**
   * Returns a new version of `this` but with the [pinned] updated to the opposite of its previous
   * state.
   *
   * @return The [Announcement] but with [pinned] inversed.
   */
  fun togglePin(): Announcement {
    return copy(pinned = !pinned)
  }
}
