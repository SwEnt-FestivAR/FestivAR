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
}
