// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingShadowTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** The pixels of the white canvas drawn in [drawShadows]. */
  private lateinit var pixels: PixelMap

  /**
   * Draws, on a white 320x160 canvas, two white 40x40 buttons casting a black [floatingShadow]: a
   * circle one (the default shape) spanning 60 to 100 on both axes, and a square one spanning 220
   * to 260 horizontally and 60 to 100 vertically.
   */
  @Before
  fun drawShadows() {
    composeTestRule.setContent {
      // A density of 1, so that a dp is a pixel and the positions in the tests are exact
      CompositionLocalProvider(LocalDensity provides Density(1f)) {
        Box(modifier = Modifier.size(320.dp, 160.dp).background(Color.White).testTag("canvas")) {
          Box(
              modifier =
                  Modifier.offset(60.dp, 60.dp)
                      .floatingShadow(Color.Black)
                      .size(40.dp)
                      .background(Color.White)
          )
          Box(
              modifier =
                  Modifier.offset(220.dp, 60.dp)
                      .floatingShadow(Color.Black, RectangleShape)
                      .size(40.dp)
                      .background(Color.White)
          )
        }
      }
    }
    pixels = composeTestRule.onNodeWithTag("canvas").captureToImage().toPixelMap()
  }

  /** Test that the shadow darkens the canvas just below the button, and not far away from it. */
  @Test
  fun shadowIsDrawnAroundTheButtonOnly() {
    assertTrue(pixels[80, 104].red < 1f)
    // Beyond the blur of the shadow, the canvas stays white
    assertEquals(Color.White, pixels[2, 2])
    assertEquals(Color.White, pixels[160, 80])
  }

  /** Test that the shadow is offset downwards: it is darker below the button than above it. */
  @Test
  fun shadowIsOffsetDownwards() {
    val above = pixels[80, 56]
    val below = pixels[80, 104]

    assertTrue("Expected $below to be darker than $above", below.red < above.red)
  }

  /**
   * Test that the shadow follows the shape of the button: a square one darkens the corner below the
   * button more than a circle one, whose shape leaves that corner out.
   */
  @Test
  fun shadowFollowsTheShapeOfTheButton() {
    val circleCorner = pixels[102, 106]
    val squareCorner = pixels[262, 106]

    assertTrue(
        "Expected $squareCorner to be darker than $circleCorner",
        squareCorner.red < circleCorner.red,
    )
  }
}
