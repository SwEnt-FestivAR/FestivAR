// Written with the help of an AI coding assistant and reviewed line by line by the author.
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * The design's soft shadow under a floating button, drawn in place of the Material elevation: a 24
 * dp blur, 8 dp below, at 14 percent of [color].
 *
 * @param shape the outline of the button the shadow follows.
 * @param color the shadow's colour; the screens pass the theme's onSurface.
 */
fun Modifier.floatingShadow(shape: Shape, color: Color): Modifier =
    dropShadow(
        shape = shape,
        shadow =
            Shadow(radius = 24.dp, color = color, offset = DpOffset(0.dp, 8.dp), alpha = 0.14f),
    )
