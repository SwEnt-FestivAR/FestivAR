// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

package com.android.festivar.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
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

  /**
   * Display, headline and title (large, medium) styles of the Figma design use Bricolage Grotesque.
   */
  @Test
  fun bricolageStyles_matchFigma() {
    val b = BricolageGrotesque
    val t = AppTypography
    assertStyle("displayLarge", t.displayLarge, b, FontWeight.Bold, 44.sp, 48.sp, (-0.02).em)
    assertStyle("displayMedium", t.displayMedium, b, FontWeight.Bold, 36.sp, 40.sp, (-0.02).em)
    assertStyle("displaySmall", t.displaySmall, b, FontWeight.Bold, 32.sp, 36.sp, (-0.02).em)
    assertStyle("headlineLarge", t.headlineLarge, b, FontWeight.SemiBold, 28.sp, 32.sp, (-0.01).em)
    assertStyle(
        "headlineMedium",
        t.headlineMedium,
        b,
        FontWeight.SemiBold,
        24.sp,
        28.sp,
        (-0.01).em,
    )
    assertStyle("headlineSmall", t.headlineSmall, b, FontWeight.SemiBold, 22.sp, 28.sp, (-0.01).em)
    assertStyle("titleLarge", t.titleLarge, b, FontWeight.SemiBold, 22.sp, 28.sp, (-0.01).em)
    assertStyle("titleMedium", t.titleMedium, b, FontWeight.SemiBold, 18.sp, 24.sp, 0.em)
  }

  /** Title small, body and label styles of the Figma design use Figtree. */
  @Test
  fun figtreeStyles_matchFigma() {
    val f = Figtree
    val t = AppTypography
    assertStyle("titleSmall", t.titleSmall, f, FontWeight.SemiBold, 16.sp, 24.sp, 0.em)
    assertStyle("bodyLarge", t.bodyLarge, f, FontWeight.Normal, 16.sp, 24.sp, 0.em)
    assertStyle("bodyMedium", t.bodyMedium, f, FontWeight.Normal, 14.sp, 20.sp, 0.em)
    assertStyle("bodySmall", t.bodySmall, f, FontWeight.Normal, 13.sp, 16.sp, 0.em)
    assertStyle("labelLarge", t.labelLarge, f, FontWeight.SemiBold, 16.sp, 24.sp, 0.em)
    assertStyle("labelMedium", t.labelMedium, f, FontWeight.Medium, 13.sp, 16.sp, 0.01.em)
    assertStyle("labelSmall", t.labelSmall, f, FontWeight.SemiBold, 13.sp, 16.sp, 0.04.em)
  }

  /** Numeral styles have no Material slot and are exposed on their own. */
  @Test
  fun numeralStyles_matchFigma() {
    val f = Figtree
    assertStyle("numeralLarge", NumeralLarge, f, FontWeight.SemiBold, 32.sp, 36.sp, (-0.01).em)
    assertStyle("numeralMedium", NumeralMedium, f, FontWeight.SemiBold, 16.sp, 24.sp, 0.em)
    assertStyle("numeralSmall", NumeralSmall, f, FontWeight.SemiBold, 13.sp, 16.sp, 0.em)
  }

  private fun opticalSizes(family: FontFamily): List<Float> =
      (family as List<*>).filterIsInstance<ResourceFont>().flatMap { font ->
        font.variationSettings.settings
            .filter { it.axisName == "opsz" }
            .map { it.toVariationValue(Density(1f)) }
      }

  /** Figma sets the optical size of every Bricolage Grotesque weight to 14. */
  @Test
  fun bricolageOpticalSize_isFourteen() {
    assertEquals(List(4) { 14f }, opticalSizes(BricolageGrotesque))
  }

  /** Figtree has no optical size in the Figma design, so it keeps the font default. */
  @Test
  fun figtreeOpticalSize_isNotSet() {
    assertEquals(emptyList<Float>(), opticalSizes(Figtree))
  }
}
