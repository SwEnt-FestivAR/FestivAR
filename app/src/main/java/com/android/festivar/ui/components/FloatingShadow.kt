// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * The soft floating shadow of the design, drawn below a floating button instead of the Material
 * elevation. Give the button a zero elevation so the two do not add up.
 *
 * @param color The colour of the shadow, the theme's `onSurface` in the design.
 * @param shape The shape of the button casting the shadow.
 */
fun Modifier.floatingShadow(color: Color, shape: Shape = CircleShape): Modifier =
    dropShadow(
        shape = shape,
        shadow =
            Shadow(radius = 24.dp, color = color, offset = DpOffset(0.dp, 8.dp), alpha = 0.14f),
    )
