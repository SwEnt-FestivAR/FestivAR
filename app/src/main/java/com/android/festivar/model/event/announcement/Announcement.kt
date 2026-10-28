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
 *
 * @property announcementId Unique identifier of the [Announcement].
 * @property title Name/subject visible of the [Announcement].
 * @property authorId Unique identifier of the user author of the [Announcement].
 * @property description Free-text description, content-body of the [Announcement].
 * @property priority How urgent and important the [Announcement] is.
 * @property updatedAt Time of the creation of the [Announcement] or of the last modification.
 * @property links Useful links attached to the [Announcement].
 * @property pinned States if the [Announcement] is pinned or not.
 */
data class Announcement(
    val announcementId: String,
    val title: String,
    val authorId: String,
    val description: String = "",
    val priority: Priority = Priority.NONE,
    val updatedAt: ZonedDateTime,
    val links: List<String> = emptyList(),
    val pinned: Boolean = false,
) {

  init {
    require(announcementId.isNotBlank()) { "The announcementId cannot be empty." }
    require(title.isNotBlank()) { "The title cannot be empty." }
    require(authorId.isNotBlank()) { "The authorId cannot be empty." }
    require(links.none { it.isBlank() }) { "A link cannot be empty." }
    require(links.distinct().size == links.size) { "A link is duplicated." }
  }

  /** Utility function that checks that a modification at [at] is not before the last one.
   *
   * @param at The time of the modification
   * @throws IllegalArgumentException if [at] strictly before [updatedAt].
   */
  private fun requireValidEditTime(at: ZonedDateTime) =
    require(!at.isBefore(updatedAt)) { "The modification cannot be before the previous one." }

  /**
   * Returns a new version of `this` but with the [title] changed with the given parameter, so an
   * edited version.
   *
   * @param newTitle The new [title] for the edited [Announcement].
   * @param actualTime The time at which the last modification was done.
   * @return The [Announcement] but with [title] replaced by [newTitle].
   * @throws IllegalArgumentException If the given [newTitle] is empty or if [actualTime] strictly before
   * [updatedAt].
   */
  fun editTitle(newTitle: String, actualTime: ZonedDateTime): Announcement {
    require(newTitle.isNotBlank()) { "The title cannot be empty." }
    requireValidEditTime(actualTime)
    return copy(title = newTitle, updatedAt = actualTime)
  }

  /**
   * Returns a new version of `this` but with the [description] changed with the given parameter, so
   * an edited version.
   *
   * @param newDescription The new [description] for the edited [Announcement].
   * @param actualTime The time at which the last modification was done.
   * @return The [Announcement] but with [description] replaced by [newDescription].
   */
  fun editDescription(newDescription: String, actualTime: ZonedDateTime): Announcement {
    requireValidEditTime(actualTime)
    return copy(description = newDescription, updatedAt = actualTime)
  }

  /**
   * Adds a new link to the [Announcement] by returning a version of `this` with [link] added to the
   * [links] list.
   *
   * @param link The link that is going to be added to `this`, to be added to [links] list.
   * @param actualTime The time at which the last modification was done.
   * @return The [Announcement] but with [link] added to it.
   * @throws IllegalArgumentException if the given [link] is blank or if it is a duplicate or if [actualTime] strictly before
   * [updatedAt].
   */
  fun addLink(link: String, actualTime: ZonedDateTime): Announcement {
    require(link.isNotBlank()) { "A link cannot be empty." }
    require(!links.contains(link)) { "The link already exists." }
    requireValidEditTime(actualTime)
    return copy(links = links + link, updatedAt = actualTime)
  }

  /**
   * Removes a link from the [links] by returning a version of `this` with [link] removed from the
   * [links] list.
   *
   * @param link The link that is going to be removed from `this`, from [links] list.
   * @param actualTime The time at which the last modification was done.
   * @return The [Announcement] but with [link] removed from it.
   * @throws IllegalArgumentException If [link] is not present in the [Announcement] or if [actualTime] strictly before
   * [updatedAt].
   */
  fun removeLink(link: String, actualTime: ZonedDateTime): Announcement {
    require(links.any { it == link }) { "This link is not present in this Announcement." }
    requireValidEditTime(actualTime)
    return copy(links = links - link, updatedAt = actualTime)
  }

  /**
   * Returns a new version of `this` but with the [links] changed with the given parameter, so an
   * edited version.
   *
   * @param newLinks The new [links] for the edited [Announcement].
   * @param actualTime The time at which the last modification was done.
   * @return The [Announcement] but with [links] replaced by [newLinks].
   * @throws IllegalArgumentException If the given links are invalid or that there is duplicates or if [actualTime] strictly before
   * [updatedAt].
   */
  fun editLinks(newLinks: List<String>, actualTime: ZonedDateTime): Announcement {
    require(newLinks.none { it.isBlank() }) { "A link cannot be empty." }
    require(newLinks.distinct().size == newLinks.size) { "A link is duplicated." }
    requireValidEditTime(actualTime)
    return copy(links = newLinks, updatedAt = actualTime)
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
