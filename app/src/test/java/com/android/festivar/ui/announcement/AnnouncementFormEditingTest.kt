// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.FieldError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests of the field rules of [AnnouncementFormState], and of [AnnouncementFormEditing] over a
 * plain state, without a ViewModel.
 */
class AnnouncementFormEditingTest {
  private val state = MutableStateFlow(AnnouncementFormState())
  private val editing =
      AnnouncementFormEditing().also { it.applyEdit = { change -> state.update(change) } }

  // Applies one setter and checks that only the expected fields changed.
  private fun assertOnly(
      expected: (AnnouncementFormState) -> AnnouncementFormState,
      setter: () -> Unit,
  ) {
    val before = state.value
    setter()
    assertEquals(expected(before), state.value)
  }

  // Breaks if an error does not wait for its field to be edited, so a new form opens with errors.
  @Test
  fun anUntouchedEmptyFormShowsNoError() {
    val form = AnnouncementFormState()

    assertNull(form.titleError)
    assertNull(form.contentError)
  }

  // Breaks if titleError reads the content or its edited flag, counts whitespace as a title, or
  // reports the content error.
  @Test
  fun anEditedBlankTitleShowsTheTitleErrorOnly() {
    val form = AnnouncementFormState(title = "  ", titleEdited = true)

    assertEquals(FieldError.EMPTY_ANNOUNCEMENT_TITLE, form.titleError)
    assertNull(form.contentError)
    assertNull(form.copy(title = "Gates open").titleError)
  }

  // Breaks if contentError reads the title or its edited flag, counts whitespace as a content, or
  // reports the title error.
  @Test
  fun anEditedBlankContentShowsTheContentErrorOnly() {
    val form = AnnouncementFormState(content = " \n", contentEdited = true)

    assertEquals(FieldError.EMPTY_ANNOUNCEMENT_CONTENT, form.contentError)
    assertNull(form.titleError)
    assertNull(form.copy(content = "The north gate opens at 16:00.").contentError)
  }

  // Breaks if isValid accepts a blank title or a blank content, or depends on the edited flags.
  @Test
  fun isValidNeedsBothTheTitleAndTheContentNotBlank() {
    val filled = AnnouncementFormState(title = "Gates open", content = "At 16:00.")

    assertTrue(filled.isValid)
    assertFalse(filled.copy(title = " ").isValid)
    assertFalse(filled.copy(content = " ").isValid)
  }

  // Breaks if updateTitle drops the text, does not mark the title as edited, or marks the content.
  @Test
  fun updateTitleSetsTheTitleAndMarksOnlyItAsEdited() =
      assertOnly({ it.copy(title = "Gates open", titleEdited = true) }) {
        editing.updateTitle("Gates open")
      }

  // Breaks if updateContent drops the text, does not mark the content as edited, or marks the
  // title.
  @Test
  fun updateContentSetsTheContentAndMarksOnlyItAsEdited() =
      assertOnly({ it.copy(content = "At 16:00.", contentEdited = true) }) {
        editing.updateContent("At 16:00.")
      }

  // Breaks if updatePriority ignores the priority or marks a text field as edited.
  @Test
  fun updatePriorityChangesOnlyThePriority() =
      assertOnly({ it.copy(priority = AnnouncementPriority.URGENT) }) {
        editing.updatePriority(AnnouncementPriority.URGENT)
      }
}
