// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

package com.android.festivar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.festivar.R

/**
 * Builds one entry per weight from a variable font file. Extra axes (e.g. optical size) are applied
 * to every weight. Variation settings need API 26+, and the app's minSdk is 28.
 */
private fun variableFonts(
    resId: Int,
    weights: List<FontWeight>,
    vararg axes: FontVariation.Setting,
): List<Font> = weights.map { weight ->
  Font(
      resId,
      weight,
      variationSettings = FontVariation.Settings(weight, FontStyle.Normal, *axes),
  )
}

// Kept separate from the families so tests can inspect the variation settings.
internal val BricolageGrotesqueFonts =
    variableFonts(
        R.font.bricolage_grotesque,
        listOf(FontWeight.SemiBold, FontWeight.Bold),
        FontVariation.Setting("opsz", 14f),
    )

internal val FigtreeFonts =
    variableFonts(
        R.font.figtree,
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold),
    )

/** Font used for the display, headline and title styles. Figma sets its optical size to 14. */
val BricolageGrotesque = FontFamily(BricolageGrotesqueFonts)

/** Font used for the body, label and numeral styles. */
val Figtree = FontFamily(FigtreeFonts)

// Every text style of the Figma design is defined on its own, so that each one maps to exactly one
// Figma style and can change independently. Letter spacing is a percentage of the font size in
// Figma, so it is expressed in em here (-2% = -0.02).
private fun textStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit,
) =
    TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
    )

val AppTypography =
    Typography(
        displayLarge = textStyle(BricolageGrotesque, FontWeight.Bold, 44.sp, 48.sp, (-0.02).em),
        displayMedium = textStyle(BricolageGrotesque, FontWeight.Bold, 36.sp, 40.sp, (-0.02).em),
        displaySmall = textStyle(BricolageGrotesque, FontWeight.Bold, 32.sp, 36.sp, (-0.02).em),
        headlineLarge =
            textStyle(BricolageGrotesque, FontWeight.SemiBold, 28.sp, 32.sp, (-0.01).em),
        headlineMedium =
            textStyle(BricolageGrotesque, FontWeight.SemiBold, 24.sp, 28.sp, (-0.01).em),
        headlineSmall =
            textStyle(BricolageGrotesque, FontWeight.SemiBold, 22.sp, 28.sp, (-0.01).em),
        titleLarge = textStyle(BricolageGrotesque, FontWeight.SemiBold, 22.sp, 28.sp, (-0.01).em),
        titleMedium = textStyle(BricolageGrotesque, FontWeight.SemiBold, 18.sp, 24.sp, 0.em),
        titleSmall = textStyle(Figtree, FontWeight.SemiBold, 16.sp, 24.sp, 0.em),
        bodyLarge = textStyle(Figtree, FontWeight.Normal, 16.sp, 24.sp, 0.em),
        bodyMedium = textStyle(Figtree, FontWeight.Normal, 14.sp, 20.sp, 0.em),
        bodySmall = textStyle(Figtree, FontWeight.Normal, 13.sp, 16.sp, 0.em),
        labelLarge = textStyle(Figtree, FontWeight.SemiBold, 16.sp, 24.sp, 0.em),
        labelMedium = textStyle(Figtree, FontWeight.Medium, 13.sp, 16.sp, 0.01.em),
        labelSmall = textStyle(Figtree, FontWeight.SemiBold, 13.sp, 16.sp, 0.04.em),
    )

// Numeral styles have no Material slot, so they are exposed on their own.
val NumeralLarge = textStyle(Figtree, FontWeight.SemiBold, 32.sp, 36.sp, (-0.01).em)
val NumeralMedium = textStyle(Figtree, FontWeight.SemiBold, 16.sp, 24.sp, 0.em)
val NumeralSmall = textStyle(Figtree, FontWeight.SemiBold, 13.sp, 16.sp, 0.em)
