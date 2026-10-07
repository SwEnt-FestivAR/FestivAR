// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

package com.android.festivar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
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

// Text styles of the Figma design. Letter spacing is a percentage of the font size in Figma, so it
// is expressed in em here (-2% = -0.02.em).
private val displayLarge =
    TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.02).em,
    )
private val displayMedium = displayLarge.copy(fontSize = 36.sp, lineHeight = 40.sp)
private val displaySmall = displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp)
private val headlineLarge =
    TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    )
private val headlineMedium = headlineLarge.copy(fontSize = 24.sp, lineHeight = 28.sp)
private val headlineSmall = headlineLarge.copy(fontSize = 22.sp, lineHeight = 28.sp)
// Aliases: Figma defines these styles with the same values, so keep them in sync if it changes.
private val titleLarge = headlineSmall
private val titleMedium =
    headlineLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.em)
private val titleSmall =
    TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.em,
    )
private val bodyLarge = titleSmall.copy(fontWeight = FontWeight.Normal)
private val bodyMedium = bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp)
private val bodySmall = bodyLarge.copy(fontSize = 13.sp, lineHeight = 16.sp)
private val labelLarge = titleSmall
private val labelMedium =
    titleSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.em,
    )
private val labelSmall = labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.04.em)

val AppTypography =
    Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )

// Numeral styles have no Material slot, so they are exposed on their own.
val NumeralLarge = titleSmall.copy(fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em)
// Same values as titleSmall in Figma (see the note on titleLarge).
val NumeralMedium = titleSmall
val NumeralSmall = titleSmall.copy(fontSize = 13.sp, lineHeight = 16.sp)
