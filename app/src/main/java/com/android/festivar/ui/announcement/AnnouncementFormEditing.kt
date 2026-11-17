// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.FieldError

/**
 * How long a save of an announcement may last before it is reported as failed. Shared by the create
 * and edit ViewModels.
 */
internal const val ANNOUNCEMENT_SAVE_TIMEOUT_MS = 15_000L

/**
 * The part of an announcement form shared by the create and edit screens: what the user types, plus
 * the field rules. It has no saving or error flags, which belong to the state of each screen. The
 * Create announcement form does not use it yet.
 *
 * @property title The title as typed, trimmed when the announcement is saved.
 * @property content The body of the announcement as typed, trimmed when it is saved.
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
}

/** The setters an announcement form screen needs, shared by the create and edit ViewModels. */
interface AnnouncementFormEditor {
  /** Sets the title and marks it as edited, so that an empty title is reported. */
  fun updateTitle(title: String)

  /** Sets the content and marks it as edited, so that an empty content is reported. */
  fun updateContent(content: String)

  /** Sets the priority. */
  fun updatePriority(priority: AnnouncementPriority)
}

/**
 * The one implementation of [AnnouncementFormEditor], over a form it does not own: the owning
 * ViewModel decides how a change reaches its state, and when it is ignored.
 */
internal class AnnouncementFormEditing : AnnouncementFormEditor {
  /**
   * Applies a change of a form field. Set by the owning ViewModel in its init, before any setter
   * runs.
   */
  lateinit var applyEdit: ((AnnouncementFormState) -> AnnouncementFormState) -> Unit

  override fun updateTitle(title: String) = applyEdit { it.copy(title = title, titleEdited = true) }

  override fun updateContent(content: String) = applyEdit {
    it.copy(content = content, contentEdited = true)
  }

  override fun updatePriority(priority: AnnouncementPriority) = applyEdit {
    it.copy(priority = priority)
  }
}
