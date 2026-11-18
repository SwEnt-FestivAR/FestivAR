// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

/**
 * A position on the ground of a site, in metres from the origin of the site. Sizes, rotations and
 * the AR all work in metres, so everything placed on a site is stored this way; [toGeo] turns it
 * into a [GeoPosition] to draw it on a map.
 *
 * Rule, checked in `init`: both values are finite.
 *
 * @property x Metres east of the origin, negative to the west.
 * @property y Metres north of the origin, negative to the south.
 */
data class LocalPosition(val x: Double, val y: Double) {
  init {
    require(x.isFinite() && y.isFinite()) { "x and y must be finite." }
  }
}
