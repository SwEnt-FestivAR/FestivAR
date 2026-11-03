// Co-authored-by: Gemini
package com.android.festivar.ui.authentication

import com.android.festivar.model.authentication.maskEmail
import org.junit.Assert.assertEquals
import org.junit.Test

class EmailHelpersTest {

  @Test
  fun maskEmailWorksCorrectly() {
    assertEquals("sk*****@toilet.com", maskEmail("skibidi@toilet.com"))
    assertEquals("jo**@example.com", maskEmail("john@example.com"))
    assertEquals("ab*@a.co", maskEmail("abc@a.co"))
    assertEquals("ab@example.com", maskEmail("ab@example.com"))
    assertEquals("a@example.com", maskEmail("a@example.com"))
    assertEquals("invalid-email", maskEmail("invalid-email"))
  }
}
