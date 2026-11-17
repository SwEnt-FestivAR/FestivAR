// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.announcement

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.FieldError
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests of the field rules, trimming, [AnnouncementFormState.toAnnouncement] and
 * [AnnouncementFormState.matches], and of [AnnouncementFormEditing] over a plain state, without a
 * ViewModel.
 */
class AnnouncementFormEditingTest {
  private val publishedAt = ZonedDateTime.of(2026, 7, 4, 16, 0, 0, 0, ZoneId.of("Europe/Zurich"))
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

  // Breaks if a trimmed value keeps the spaces or line breaks around the text, or removes the ones
  // inside it.
  @Test
  fun theTrimmedValuesDropOnlyTheSpacesAroundTheText() {
    val form = AnnouncementFormState(title = "  Gates  open \n", content = "\tAt 16:00. \n")

    assertEquals("Gates  open", form.trimmedTitle)
    assertEquals("At 16:00.", form.trimmedContent)
  }

  // Breaks if toAnnouncement saves the untrimmed texts, drops the priority, or swaps the id, the
  // author or the time.
  @Test
  fun toAnnouncementBuildsTheAnnouncementFromTheTrimmedForm() {
    val form =
        AnnouncementFormState(
            title = " Gates open ",
            content = " At 16:00.\n",
            priority = AnnouncementPriority.URGENT,
        )

    assertEquals(
        Announcement(
            announcementId = "a1",
            title = "Gates open",
            authorId = "u1",
            content = "At 16:00.",
            priority = AnnouncementPriority.URGENT,
            updatedAt = publishedAt,
        ),
        form.toAnnouncement("a1", "u1", publishedAt),
    )
  }

  // Breaks if toAnnouncement builds an announcement from a form that cannot be saved.
  @Test
  fun toAnnouncementRejectsABlankTitleOrContent() {
    val filled = AnnouncementFormState(title = "Gates open", content = "At 16:00.")

    assertThrows(IllegalArgumentException::class.java) {
      filled.copy(title = " ").toAnnouncement("a1", "u1", publishedAt)
    }
    assertThrows(IllegalArgumentException::class.java) {
      filled.copy(content = " ").toAnnouncement("a1", "u1", publishedAt)
    }
  }

  // Breaks if matches compares the untrimmed texts, ignores one of the three fields, or compares a
  // field the form does not set (author, time zone, links, pin).
  @Test
  fun matchesComparesOnlyTheFieldsTheFormSets() {
    val stored =
        Announcement(
            announcementId = "a1",
            title = "Gates open",
            authorId = "someone-else",
            content = "At 16:00.",
            priority = AnnouncementPriority.URGENT,
            updatedAt = publishedAt.withZoneSameInstant(ZoneOffset.UTC),
            links = listOf("https://festivar.example/map"),
            pinned = true,
        )
    val form =
        AnnouncementFormState(
            title = " Gates open ",
            content = "At 16:00.\n",
            priority = AnnouncementPriority.URGENT,
        )

    assertTrue(form.matches(stored))
    assertFalse(form.copy(title = "Gates closed").matches(stored))
    assertFalse(form.copy(content = "At 17:00.").matches(stored))
    assertFalse(form.copy(priority = AnnouncementPriority.INFO).matches(stored))
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
