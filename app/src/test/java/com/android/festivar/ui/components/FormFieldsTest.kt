// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import com.android.festivar.R
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
