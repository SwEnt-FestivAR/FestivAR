// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import com.android.festivar.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests of the field error messages of the forms. */
class FormFieldsTest {
  // Breaks if the two announcement errors swap or lose their strings.
  @Test
  fun emptyAnnouncementTitleMapsToItsString() {
    assertEquals(
        R.string.field_error_empty_announcement_title,
        fieldErrorMessageRes(FieldError.EMPTY_ANNOUNCEMENT_TITLE),
    )
  }

  // Breaks if the two announcement errors swap or lose their strings.
  @Test
  fun emptyAnnouncementContentMapsToItsString() {
    assertEquals(
        R.string.field_error_empty_announcement_content,
        fieldErrorMessageRes(FieldError.EMPTY_ANNOUNCEMENT_CONTENT),
    )
  }

  // Breaks if TOO_FEW_SLOTS is mapped to another message.
  @Test
  fun tooFewSlotsMapsToItsString() {
    assertEquals(
        R.string.field_error_too_few_slots,
        fieldErrorMessageRes(FieldError.TOO_FEW_SLOTS),
    )
  }

  // A text saved before the limit can be edited down: breaks if it is cut back to maxLength.
  @Test
  fun limitEdit_keepsALongerSavedTextThatIsEditedDown() {
    assertEquals("abcdef", limitEdit(old = "abcdefg", new = "abcdef", maxLength = 5))
  }

  // ...but it cannot grow: breaks if typing at its end is accepted.
  @Test
  fun limitEdit_doesNotLetALongerSavedTextGrow() {
    assertEquals("abcdefg", limitEdit(old = "abcdefg", new = "abcdefgh", maxLength = 5))
  }

  // Pasting in the middle keeps what fits of the paste and all the text around it: breaks if the
  // end of the text is cut instead, or the whole paste is dropped.
  @Test
  fun limitEdit_pastedInTheMiddle_keepsOnlyWhatFitsOfThePaste() {
    assertEquals("aXYbc", limitEdit(old = "abc", new = "aXYZbc", maxLength = 5))
  }

  // Pasting over a selection (here all of the text) cuts the paste: breaks if it is dropped.
  @Test
  fun limitEdit_pastedOverASelection_cutsThePaste() {
    assertEquals("01234", limitEdit(old = "abc", new = "0123456789", maxLength = 5))
  }

  // With repeated letters, the shared start and end overlap ("aaa" is both); counting them twice
  // would cut a negative range and crash.
  @Test
  fun limitEdit_repeatedLetterAtTheLimit_isDropped() {
    assertEquals("aaa", limitEdit(old = "aaa", new = "aaaa", maxLength = 3))
  }
}
