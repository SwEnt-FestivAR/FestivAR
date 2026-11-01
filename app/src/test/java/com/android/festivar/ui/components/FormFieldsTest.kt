// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import com.android.festivar.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests of the field error messages of the forms. */
class FormFieldsTest {
  // Breaks if TOO_FEW_SLOTS is mapped to another message.
  @Test
  fun tooFewSlotsMapsToItsString() {
    assertEquals(
        R.string.field_error_too_few_slots,
        fieldErrorMessageRes(FieldError.TOO_FEW_SLOTS),
    )
  }
}
