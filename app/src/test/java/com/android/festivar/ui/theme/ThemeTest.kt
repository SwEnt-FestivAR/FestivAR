// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class ThemeTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** Every color role of the light scheme, with the AppTheme color it must have. */
  private fun expectedLightColors(scheme: ColorScheme): List<Triple<String, Color, Color>> =
      listOf(
          Triple("primary", primaryLight, scheme.primary),
          Triple("onPrimary", onPrimaryLight, scheme.onPrimary),
          Triple("primaryContainer", primaryContainerLight, scheme.primaryContainer),
          Triple("onPrimaryContainer", onPrimaryContainerLight, scheme.onPrimaryContainer),
          Triple("secondary", secondaryLight, scheme.secondary),
          Triple("onSecondary", onSecondaryLight, scheme.onSecondary),
          Triple("secondaryContainer", secondaryContainerLight, scheme.secondaryContainer),
          Triple("onSecondaryContainer", onSecondaryContainerLight, scheme.onSecondaryContainer),
          Triple("tertiary", tertiaryLight, scheme.tertiary),
          Triple("onTertiary", onTertiaryLight, scheme.onTertiary),
          Triple("tertiaryContainer", tertiaryContainerLight, scheme.tertiaryContainer),
          Triple("onTertiaryContainer", onTertiaryContainerLight, scheme.onTertiaryContainer),
          Triple("error", errorLight, scheme.error),
          Triple("onError", onErrorLight, scheme.onError),
          Triple("errorContainer", errorContainerLight, scheme.errorContainer),
          Triple("onErrorContainer", onErrorContainerLight, scheme.onErrorContainer),
          Triple("background", backgroundLight, scheme.background),
          Triple("onBackground", onBackgroundLight, scheme.onBackground),
          Triple("surface", surfaceLight, scheme.surface),
          Triple("onSurface", onSurfaceLight, scheme.onSurface),
          Triple("surfaceVariant", surfaceVariantLight, scheme.surfaceVariant),
          Triple("onSurfaceVariant", onSurfaceVariantLight, scheme.onSurfaceVariant),
          Triple("outline", outlineLight, scheme.outline),
          Triple("outlineVariant", outlineVariantLight, scheme.outlineVariant),
          Triple("scrim", scrimLight, scheme.scrim),
          Triple("inverseSurface", inverseSurfaceLight, scheme.inverseSurface),
          Triple("inverseOnSurface", inverseOnSurfaceLight, scheme.inverseOnSurface),
          Triple("inversePrimary", inversePrimaryLight, scheme.inversePrimary),
          Triple("surfaceTint", surfaceTintLight, scheme.surfaceTint),
          Triple("surfaceDim", surfaceDimLight, scheme.surfaceDim),
          Triple("surfaceBright", surfaceBrightLight, scheme.surfaceBright),
          Triple(
              "surfaceContainerLowest",
              surfaceContainerLowestLight,
              scheme.surfaceContainerLowest,
          ),
          Triple("surfaceContainerLow", surfaceContainerLowLight, scheme.surfaceContainerLow),
          Triple("surfaceContainer", surfaceContainerLight, scheme.surfaceContainer),
          Triple("surfaceContainerHigh", surfaceContainerHighLight, scheme.surfaceContainerHigh),
          Triple(
              "surfaceContainerHighest",
              surfaceContainerHighestLight,
              scheme.surfaceContainerHighest,
          ),
      )

  /** Every color role of the dark scheme, with the AppTheme color it must have. */
  private fun expectedDarkColors(scheme: ColorScheme): List<Triple<String, Color, Color>> =
      listOf(
          Triple("primary", primaryDark, scheme.primary),
          Triple("onPrimary", onPrimaryDark, scheme.onPrimary),
          Triple("primaryContainer", primaryContainerDark, scheme.primaryContainer),
          Triple("onPrimaryContainer", onPrimaryContainerDark, scheme.onPrimaryContainer),
          Triple("secondary", secondaryDark, scheme.secondary),
          Triple("onSecondary", onSecondaryDark, scheme.onSecondary),
          Triple("secondaryContainer", secondaryContainerDark, scheme.secondaryContainer),
          Triple("onSecondaryContainer", onSecondaryContainerDark, scheme.onSecondaryContainer),
          Triple("tertiary", tertiaryDark, scheme.tertiary),
          Triple("onTertiary", onTertiaryDark, scheme.onTertiary),
          Triple("tertiaryContainer", tertiaryContainerDark, scheme.tertiaryContainer),
          Triple("onTertiaryContainer", onTertiaryContainerDark, scheme.onTertiaryContainer),
          Triple("error", errorDark, scheme.error),
          Triple("onError", onErrorDark, scheme.onError),
          Triple("errorContainer", errorContainerDark, scheme.errorContainer),
          Triple("onErrorContainer", onErrorContainerDark, scheme.onErrorContainer),
          Triple("background", backgroundDark, scheme.background),
          Triple("onBackground", onBackgroundDark, scheme.onBackground),
          Triple("surface", surfaceDark, scheme.surface),
          Triple("onSurface", onSurfaceDark, scheme.onSurface),
          Triple("surfaceVariant", surfaceVariantDark, scheme.surfaceVariant),
          Triple("onSurfaceVariant", onSurfaceVariantDark, scheme.onSurfaceVariant),
          Triple("outline", outlineDark, scheme.outline),
          Triple("outlineVariant", outlineVariantDark, scheme.outlineVariant),
          Triple("scrim", scrimDark, scheme.scrim),
          Triple("inverseSurface", inverseSurfaceDark, scheme.inverseSurface),
          Triple("inverseOnSurface", inverseOnSurfaceDark, scheme.inverseOnSurface),
          Triple("inversePrimary", inversePrimaryDark, scheme.inversePrimary),
          Triple("surfaceTint", surfaceTintDark, scheme.surfaceTint),
          Triple("surfaceDim", surfaceDimDark, scheme.surfaceDim),
          Triple("surfaceBright", surfaceBrightDark, scheme.surfaceBright),
          Triple(
              "surfaceContainerLowest",
              surfaceContainerLowestDark,
              scheme.surfaceContainerLowest,
          ),
          Triple("surfaceContainerLow", surfaceContainerLowDark, scheme.surfaceContainerLow),
          Triple("surfaceContainer", surfaceContainerDark, scheme.surfaceContainer),
          Triple("surfaceContainerHigh", surfaceContainerHighDark, scheme.surfaceContainerHigh),
          Triple(
              "surfaceContainerHighest",
              surfaceContainerHighestDark,
              scheme.surfaceContainerHighest,
          ),
      )

  /** Asserts that every role has its expected color, naming the role on failure. */
  private fun assertColors(expected: List<Triple<String, Color, Color>>) {
    expected.forEach { (role, expectedColor, actualColor) ->
      assertEquals("Wrong color for $role", expectedColor, actualColor)
    }
  }

  /**
   * Renders [AppTheme] and returns the color scheme and typography it provides. If [darkTheme] is
   * `null`, the theme's default is used, i.e. the system's dark mode.
   */
  private fun renderTheme(darkTheme: Boolean? = null): Pair<ColorScheme, Typography> {
    lateinit var scheme: ColorScheme
    lateinit var typography: Typography
    composeTestRule.setContent {
      val content =
          @Composable {
            scheme = MaterialTheme.colorScheme
            typography = MaterialTheme.typography
          }
      if (darkTheme == null) AppTheme(content = content)
      else AppTheme(darkTheme = darkTheme, content = content)
    }
    composeTestRule.waitForIdle()
    return scheme to typography
  }

  /** Forcing light mode gives every color role its AppTheme light color. */
  @Test
  fun lightTheme_usesEveryAppThemeLightColor() {
    val (scheme, _) = renderTheme(darkTheme = false)
    assertColors(expectedLightColors(scheme))
  }

  /** Forcing dark mode gives every color role its AppTheme dark color. */
  @Test
  fun darkTheme_usesEveryAppThemeDarkColor() {
    val (scheme, _) = renderTheme(darkTheme = true)
    assertColors(expectedDarkColors(scheme))
  }

  /** The light and dark palettes really differ, so switching mode is visible. */
  @Test
  fun lightAndDarkThemes_haveDifferentBackgrounds() {
    assertNotEquals(backgroundLight, backgroundDark)
    assertNotEquals(onBackgroundLight, onBackgroundDark)
  }

  /** Without a parameter, the theme is light when the phone is in light mode. */
  @Test
  @Config(qualifiers = "notnight")
  fun defaultTheme_isLight_whenSystemIsInLightMode() {
    val (scheme, _) = renderTheme()
    assertColors(expectedLightColors(scheme))
  }

  /** Without a parameter, the theme is dark when the phone is in dark mode. */
  @Test
  @Config(qualifiers = "night")
  fun defaultTheme_isDark_whenSystemIsInDarkMode() {
    val (scheme, _) = renderTheme()
    assertColors(expectedDarkColors(scheme))
  }

  /** The theme provides the app's own typography, AppTypography. */
  @Test
  fun theme_usesTheAppTypography() {
    assertSame(AppTypography, renderTheme(darkTheme = false).second)
  }
}
