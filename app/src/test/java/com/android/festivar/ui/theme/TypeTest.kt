// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

package com.android.festivar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.festivar.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

  private fun opticalSizes(fonts: List<Font>): List<Float> =
      fonts.filterIsInstance<ResourceFont>().flatMap { font ->
        font.variationSettings.settings
            .filter { it.axisName == "opsz" }
            .map { it.toVariationValue(Density(1f)) }
      }

  /** Figma sets the optical size of every Bricolage Grotesque weight to 14. */
  @Test
  fun bricolageOpticalSize_isFourteen() {
    assertEquals(List(2) { 14f }, opticalSizes(BricolageGrotesqueFonts))
  }

  /** Figtree has no optical size in the Figma design, so it keeps the font default. */
  @Test
  fun figtreeOpticalSize_isNotSet() {
    assertEquals(emptyList<Float>(), opticalSizes(FigtreeFonts))
  }

  private val materialStyles: Map<String, TextStyle> =
      with(AppTypography) {
        mapOf(
            "displayLarge" to displayLarge,
            "displayMedium" to displayMedium,
            "displaySmall" to displaySmall,
            "headlineLarge" to headlineLarge,
            "headlineMedium" to headlineMedium,
            "headlineSmall" to headlineSmall,
            "titleLarge" to titleLarge,
            "titleMedium" to titleMedium,
            "titleSmall" to titleSmall,
            "bodyLarge" to bodyLarge,
            "bodyMedium" to bodyMedium,
            "bodySmall" to bodySmall,
            "labelLarge" to labelLarge,
            "labelMedium" to labelMedium,
            "labelSmall" to labelSmall,
        )
      }

  private val allStyles: Map<String, TextStyle> =
      materialStyles +
          mapOf(
              "NumeralLarge" to NumeralLarge,
              "NumeralMedium" to NumeralMedium,
              "NumeralSmall" to NumeralSmall,
          )

  private fun declaredWeights(family: FontFamily): List<FontWeight> =
      when (family) {
        BricolageGrotesque -> BricolageGrotesqueFonts
        Figtree -> FigtreeFonts
        else -> error("Unexpected font family $family")
      }.map { it.weight }

  /** A style whose weight is not declared in its family would silently fall back to another one. */
  @Test
  fun everyStyleWeight_isDeclaredInItsFontFamily() {
    allStyles.forEach { (name, style) ->
      val family = style.fontFamily ?: error("$name has no font family")
      assertTrue(
          "$name uses ${style.fontWeight} which ${declaredWeights(family)} does not provide",
          style.fontWeight in declaredWeights(family),
      )
    }
  }

  /** Every declared weight is used by at least one style, so no font entry is dead weight. */
  @Test
  fun everyDeclaredWeight_isUsedByAStyle() {
    listOf(BricolageGrotesque, Figtree).forEach { family ->
      val used = allStyles.values.filter { it.fontFamily == family }.mapNotNull { it.fontWeight }
      declaredWeights(family).forEach { weight ->
        assertTrue("$weight of $family is never used", weight in used)
      }
    }
  }

  /** The design only uses the two Figma families, so no style may fall back to the system font. */
  @Test
  fun everyStyle_usesAFigmaFontFamily() {
    allStyles.forEach { (name, style) ->
      assertTrue(
          "$name must use Bricolage Grotesque or Figtree",
          style.fontFamily == BricolageGrotesque || style.fontFamily == Figtree,
      )
    }
  }

  /** Guards against a Material slot being left on the default typography. */
  @Test
  fun appTypography_overridesEveryMaterialSlot() {
    val d = Typography()
    val defaults =
        mapOf(
            "displayLarge" to d.displayLarge,
            "displayMedium" to d.displayMedium,
            "displaySmall" to d.displaySmall,
            "headlineLarge" to d.headlineLarge,
            "headlineMedium" to d.headlineMedium,
            "headlineSmall" to d.headlineSmall,
            "titleLarge" to d.titleLarge,
            "titleMedium" to d.titleMedium,
            "titleSmall" to d.titleSmall,
            "bodyLarge" to d.bodyLarge,
            "bodyMedium" to d.bodyMedium,
            "bodySmall" to d.bodySmall,
            "labelLarge" to d.labelLarge,
            "labelMedium" to d.labelMedium,
            "labelSmall" to d.labelSmall,
        )
    assertEquals(15, materialStyles.size)
    materialStyles.forEach { (name, style) ->
      assertNotEquals("$name still has the Material default", defaults[name], style)
    }
  }

  /** Line height must leave room for the glyphs, otherwise text gets clipped. */
  @Test
  fun lineHeight_isNeverSmallerThanFontSize() {
    allStyles.forEach { (name, style) ->
      assertTrue("$name lineHeight < fontSize", style.lineHeight.value >= style.fontSize.value)
    }
  }

  /** Each entry must point at the bundled font file and carry its own weight on the wght axis. */
  @Test
  fun fontEntries_useBundledFilesAndMatchingWeightAxis() {
    listOf(
            BricolageGrotesqueFonts to R.font.bricolage_grotesque,
            FigtreeFonts to R.font.figtree,
        )
        .forEach { (fonts, resId) ->
          fonts.filterIsInstance<ResourceFont>().also { assertEquals(fonts.size, it.size) }.forEach {
              font ->
            assertEquals(resId, font.resId)
            val wght =
                font.variationSettings.settings
                    .filter { it.axisName == "wght" }
                    .map { it.toVariationValue(Density(1f)) }
            assertEquals(listOf(font.weight.weight.toFloat()), wght)
          }
        }
  }

  /** Fonts must not declare the same weight twice, Compose would pick one arbitrarily. */
  @Test
  fun fontFamilies_haveNoDuplicateWeights() {
    listOf(BricolageGrotesqueFonts, FigtreeFonts).forEach { fonts ->
      val weights = fonts.map { it.weight }
      assertEquals(weights.distinct(), weights)
    }
  }

  /** The OFL requires the license to ship with the font files. */
  @Test
  fun bundledFonts_shipTheirLicense() {
    listOf("bricolage_grotesque", "figtree").forEach { name ->
      val license = File("../third_party/fonts/$name/OFL.txt")
      assertTrue("Missing ${license.path}", license.isFile)
      assertTrue(license.readText().contains("SIL Open Font License"))
      assertTrue(File("src/main/res/font/$name.ttf").isFile)
    }
  }
}
