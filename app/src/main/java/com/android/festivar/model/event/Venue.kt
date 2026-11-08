// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

/**
 * Where the venue of an [Event] sits on the earth and how its plan is turned: the point that
 * structures and task spots are measured from, in metres, and the bearing of the plan.
 *
 * @property latitude Latitude of the venue centre, in degrees, between -90 and 90.
 * @property longitude Longitude of the venue centre, in degrees, between -180 and 180.
 * @property bearing Rotation of the plan in degrees clockwise from north, at least 0 and below 360.
 */
data class Venue(val latitude: Double, val longitude: Double, val bearing: Double = 0.0) {
  init {
    require(latitude in -90.0..90.0) { "latitude must be between -90 and 90." }
    require(longitude in -180.0..180.0) { "longitude must be between -180 and 180." }
    require(bearing >= 0.0 && bearing < 360.0) { "bearing must be at least 0 and below 360." }
  }
}
