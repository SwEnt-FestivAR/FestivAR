// Co-authored-by: Gemini
package com.android.festivar.ui.authentication

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthHelpersTest {

  @Test
  fun maskEmailWorksCorrectly() {
    assertEquals("sk*****@toilet.com", maskEmail("skibidi@toilet.com"))
    assertEquals("jo****@example.com", maskEmail("john@example.com"))
    assertEquals("ab*@a.co", maskEmail("abc@a.co"))
  }
}
