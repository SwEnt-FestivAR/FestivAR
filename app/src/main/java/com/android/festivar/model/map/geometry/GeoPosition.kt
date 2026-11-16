// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

/**
 * A position on Earth. Only the origin of a site and its boundary are stored this way; everything
 * placed on the site is a [LocalPosition] in metres from the origin.
 *
 * Rules, checked in `init`: both values are finite, [latitude] is within -90..90 and [longitude]
 * within -180..180.
 *
 * @property latitude Degrees north of the equator, negative to the south.
 * @property longitude Degrees east of the Greenwich meridian, negative to the west.
 */
data class GeoPosition(val latitude: Double, val longitude: Double) {
  init {
    require(latitude.isFinite() && longitude.isFinite()) {
      "The latitude and the longitude must be finite."
    }
    require(latitude in -90.0..90.0) { "The latitude must be between -90 and 90." }
    require(longitude in -180.0..180.0) { "The longitude must be between -180 and 180." }
  }
}
