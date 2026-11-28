// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.chat

import com.android.festivar.model.chat.CHAT_NAME_MAX_LENGTH
import com.android.festivar.ui.components.FieldError

/**
 * Name field shared by create and edit chat forms.
 *
 * @property name The name as typed. The saved value is [trimmedName].
 * @property nameEdited Whether the user has edited the field.
 */
data class ChatFormState(
    val name: String = "",
    val nameEdited: Boolean = false,
) {
  /** The name error, shown only after the user edits the field. */
  val nameError: FieldError?
    get() =
        when {
          !nameEdited -> null
          trimmedName.isEmpty() -> FieldError.EMPTY_CHAT_NAME
          trimmedName.length > CHAT_NAME_MAX_LENGTH -> FieldError.TOO_LONG_CHAT_NAME
          else -> null
        }

  /** Whether the name can be saved. */
  val isValid: Boolean
    get() = trimmedName.isNotEmpty() && trimmedName.length <= CHAT_NAME_MAX_LENGTH

  /** The name without surrounding whitespace. */
  val trimmedName: String
    get() = name.trim()
}

/** The name setter shared by create and edit chat ViewModels. */
interface ChatFormEditor {
  /** Applies a form change; the ViewModel decides when edits are allowed. */
  fun applyEdit(change: (ChatFormState) -> ChatFormState)

  /** Sets the name and marks it as edited. */
  fun updateName(name: String) = applyEdit { it.copy(name = name, nameEdited = true) }
}
