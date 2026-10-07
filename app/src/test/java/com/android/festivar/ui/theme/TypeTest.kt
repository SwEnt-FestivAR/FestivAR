// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

package com.android.festivar.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {

  private fun assertStyle(
      name: String,
      style: TextStyle,
      family: FontFamily,
      weight: FontWeight,
      size: TextUnit,
      lineHeight: TextUnit,
      letterSpacing: TextUnit,
  ) {
    assertEquals("$name family", family, style.fontFamily)
    assertEquals("$name weight", weight, style.fontWeight)
    assertEquals("$name size", size, style.fontSize)
    assertEquals("$name lineHeight", lineHeight, style.lineHeight)
    assertEquals("$name letterSpacing", letterSpacing, style.letterSpacing)
  }

  /** Display, headline and title styles of the Figma design use Bricolage Grotesque. */
  @Test
  fun bricolageStyles_matchFigma() {
    val b = BricolageGrotesque
    val t = AppTypography
    assertStyle("displayMedium", t.displayMedium, b, FontWeight.Bold, 36.sp, 40.sp, (-2).sp)
    assertStyle("displaySmall", t.displaySmall, b, FontWeight.Bold, 32.sp, 36.sp, (-2).sp)
    assertStyle("headlineMedium", t.headlineMedium, b, FontWeight.SemiBold, 24.sp, 28.sp, (-1).sp)
    assertStyle("headlineSmall", t.headlineSmall, b, FontWeight.SemiBold, 22.sp, 28.sp, (-1).sp)
    assertStyle("titleLarge", t.titleLarge, b, FontWeight.SemiBold, 22.sp, 28.sp, (-1).sp)
  }

  /** Title small, body and label styles of the Figma design use Figtree. */
  @Test
  fun figtreeStyles_matchFigma() {
    val f = Figtree
    val t = AppTypography
    assertStyle("titleSmall", t.titleSmall, f, FontWeight.SemiBold, 16.sp, 24.sp, 0.sp)
    assertStyle("bodyLarge", t.bodyLarge, f, FontWeight.Normal, 16.sp, 24.sp, 0.sp)
    assertStyle("bodyMedium", t.bodyMedium, f, FontWeight.Normal, 14.sp, 20.sp, 0.sp)
    assertStyle("bodySmall", t.bodySmall, f, FontWeight.Normal, 13.sp, 16.sp, 0.sp)
    assertStyle("labelLarge", t.labelLarge, f, FontWeight.SemiBold, 16.sp, 24.sp, 0.sp)
    assertStyle("labelMedium", t.labelMedium, f, FontWeight.Medium, 13.sp, 16.sp, 1.sp)
    assertStyle("labelSmall", t.labelSmall, f, FontWeight.SemiBold, 13.sp, 16.sp, 4.sp)
  }

  /** Numeral styles have no Material slot and are exposed on their own. */
  @Test
  fun numeralStyles_matchFigma() {
    val f = Figtree
    assertStyle("numeralLarge", NumeralLarge, f, FontWeight.SemiBold, 32.sp, 36.sp, (-1).sp)
    assertStyle("numeralMedium", NumeralMedium, f, FontWeight.SemiBold, 16.sp, 24.sp, 0.sp)
    assertStyle("numeralSmall", NumeralSmall, f, FontWeight.SemiBold, 13.sp, 16.sp, 0.sp)
  }
}
