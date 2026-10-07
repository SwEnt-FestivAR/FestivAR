// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

package com.android.festivar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.festivar.R

private val weights =
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** Builds a family from a variable font file, with one entry per weight used in the design. */
private fun variableFamily(resId: Int): FontFamily =
    FontFamily(
        weights.map { weight ->
          Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        })

/** Font used for the display, headline and title styles. */
val BricolageGrotesque = variableFamily(R.font.bricolage_grotesque)

/** Font used for the body, label and numeral styles. */
val Figtree = variableFamily(R.font.figtree)

// Text styles of the Figma design. Letter spacing is in sp, like the other metrics.
private val displayMedium =
    TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-2).sp,
    )
private val displaySmall = displayMedium.copy(fontSize = 32.sp, lineHeight = 36.sp)
private val headlineMedium =
    TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-1).sp,
    )
private val headlineSmall = headlineMedium.copy(fontSize = 22.sp)
private val titleLarge = headlineSmall
private val titleSmall =
    TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
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
        letterSpacing = 1.sp,
    )
private val labelSmall = labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)

/** Material slots without a Figma style (displayLarge, headlineLarge, titleMedium) keep defaults. */
val AppTypography =
    Typography().copy(
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )

// Numeral styles have no Material slot, so they are exposed on their own.
val NumeralLarge =
    titleSmall.copy(fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-1).sp)
val NumeralMedium = titleSmall
val NumeralSmall = titleSmall.copy(fontSize = 13.sp, lineHeight = 16.sp)
