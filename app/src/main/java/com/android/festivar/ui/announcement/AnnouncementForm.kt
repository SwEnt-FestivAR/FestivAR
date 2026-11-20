// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.FieldError
import java.time.ZonedDateTime

/**
 * How long a save of an announcement may last before it is reported as failed. Shared by the create
 * and edit ViewModels.
 */
internal const val ANNOUNCEMENT_SAVE_TIMEOUT_MS = 15_000L

/**
 * The part of an announcement form shared by the create and edit screens: what the user types, plus
 * the field rules. It has no saving or error flags, which belong to the state of each screen. The
 * create and edit ViewModels both hold it in their UI state.
 *
 * @property title The title as typed. It is saved as [trimmedTitle].
 * @property content The body of the announcement as typed. It is saved as [trimmedContent].
 * @property priority How urgent the announcement is. [AnnouncementPriority.INFO] by default.
 * @property titleEdited Whether the title was edited, so that an empty one is reported.
 * @property contentEdited Whether the content was edited, so that an empty one is reported.
 */
data class AnnouncementFormState(
    val title: String = "",
    val content: String = "",
    val priority: AnnouncementPriority = AnnouncementPriority.INFO,
    val titleEdited: Boolean = false,
    val contentEdited: Boolean = false,
) {
  /**
   * The title error. It only appears once the title has been edited, so that an untouched form does
   * not start with an error.
   */
  val titleError: FieldError?
    get() = if (titleEdited && title.isBlank()) FieldError.EMPTY_ANNOUNCEMENT_TITLE else null

  /** The content error. Like [titleError], it only appears once the content has been edited. */
  val contentError: FieldError?
    get() = if (contentEdited && content.isBlank()) FieldError.EMPTY_ANNOUNCEMENT_CONTENT else null

  /** Whether the form can be saved: the title and the content are both required. */
  val isValid: Boolean
    get() = title.isNotBlank() && content.isNotBlank()

  /** The title as it is saved, without the spaces around it. */
  val trimmedTitle: String
    get() = title.trim()

  /** The content as it is saved, without the spaces around it. */
  val trimmedContent: String
    get() = content.trim()

  /**
   * Builds the [Announcement] described by this form, with its title and content trimmed.
   *
   * @param announcementId The id of the new announcement.
   * @param authorId The uid of the user publishing the announcement, who becomes its author.
   * @param updatedAt The time the announcement is published.
   * @throws IllegalArgumentException if the title or the content is blank, or if [announcementId]
   *   or [authorId] is blank.
   */
  fun toAnnouncement(
      announcementId: String,
      authorId: String,
      updatedAt: ZonedDateTime,
  ): Announcement {
    require(isValid) { "The form is not valid." }
    return Announcement(
        announcementId = announcementId,
        title = trimmedTitle,
        authorId = authorId,
        content = trimmedContent,
        priority = priority,
        updatedAt = updatedAt,
    )
  }

  /**
   * Whether [announcement] holds what this form saves, for example a copy read back after a failed
   * save. Only the fields the form sets are compared: full equality would also compare the time
   * zone of [Announcement.updatedAt], the links and the pin, so a copy read back from a remote
   * store would always differ.
   */
  fun matches(announcement: Announcement): Boolean =
      announcement.title == trimmedTitle &&
          announcement.content == trimmedContent &&
          announcement.priority == priority
}

/**
 * The setters an announcement form screen needs, shared by the create and edit ViewModels. A
 * ViewModel implements only [applyEdit]; the setters are written once, here.
 */
interface AnnouncementFormEditor {
  /**
   * Applies a change of a form field. The implementing ViewModel decides how the change reaches its
   * state, and when it is ignored.
   */
  fun applyEdit(change: (AnnouncementFormState) -> AnnouncementFormState)

  /** Sets the title and marks it as edited, so that an empty title is reported. */
  fun updateTitle(title: String) = applyEdit { it.copy(title = title, titleEdited = true) }

  /** Sets the content and marks it as edited, so that an empty content is reported. */
  fun updateContent(content: String) = applyEdit {
    it.copy(content = content, contentEdited = true)
  }

  /** Sets the priority. */
  fun updatePriority(priority: AnnouncementPriority) = applyEdit { it.copy(priority = priority) }
}
