// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * A plain latitude and longitude pair in degrees; the map layer converts it to the SDK type.
 *
 * @property latitude Degrees north of the equator, negative to the south.
 * @property longitude Degrees east of Greenwich, negative to the west.
 */
data class LatLngPoint(val latitude: Double, val longitude: Double)

/**
 * Venue arithmetic in metres from the venue centre, x east and y south.
 *
 * A venue is drawn on a plan: metres from its centre, x to the east and y to the south, as if north
 * were up. The plan can be turned by a bearing, in degrees clockwise, so it lines up with the real
 * site. [latLngFrom] places a plan point on the earth and [metresFrom] brings it back.
 *
 * The projection is equirectangular about the centre (R = 6 371 000 m): east metres are the
 * longitude difference in radians times cos(centre latitude) times R, south metres are minus the
 * latitude difference in radians times R. At venue scale, a few hundred metres, it is exact to a
 * centimetre.
 */
object VenueGeometry {
  private const val EARTH_RADIUS_METRES = 6_371_000.0
  private const val WALKING_METRES_PER_SECOND = 1.2

  /**
   * Plan metres (x east, y south, before rotation) to a point on the earth, the plan turned by
   * [bearingDegrees] clockwise about the centre.
   *
   * @param centerLat Latitude of the venue centre, in degrees.
   * @param centerLng Longitude of the venue centre, in degrees.
   * @param bearingDegrees How far the plan is turned clockwise from north.
   * @param x Metres east of the centre on the plan.
   * @param y Metres south of the centre on the plan.
   * @return The point on the earth.
   */
  fun latLngFrom(
      centerLat: Double,
      centerLng: Double,
      bearingDegrees: Double,
      x: Double,
      y: Double,
  ): LatLngPoint {
    val b = Math.toRadians(bearingDegrees)
    val east = x * cos(b) - y * sin(b)
    val south = x * sin(b) + y * cos(b)
    val lat = centerLat - Math.toDegrees(south / EARTH_RADIUS_METRES)
    val lng =
        centerLng + Math.toDegrees(east / (EARTH_RADIUS_METRES * cos(Math.toRadians(centerLat))))
    return LatLngPoint(lat, lng)
  }

  /**
   * The inverse of [latLngFrom]: where ([lat], [lng]) sits on the plan, in metres, before rotation.
   *
   * @param centerLat Latitude of the venue centre, in degrees.
   * @param centerLng Longitude of the venue centre, in degrees.
   * @param bearingDegrees How far the plan is turned clockwise from north.
   * @param lat Latitude of the point, in degrees.
   * @param lng Longitude of the point, in degrees.
   * @return The plan metres, x east then y south.
   */
  fun metresFrom(
      centerLat: Double,
      centerLng: Double,
      bearingDegrees: Double,
      lat: Double,
      lng: Double,
  ): Pair<Double, Double> {
    val east =
        Math.toRadians(lng - centerLng) * cos(Math.toRadians(centerLat)) * EARTH_RADIUS_METRES
    val south = -Math.toRadians(lat - centerLat) * EARTH_RADIUS_METRES
    val b = Math.toRadians(bearingDegrees)
    return (east * cos(b) + south * sin(b)) to (south * cos(b) - east * sin(b))
  }

  /**
   * Where ([lat], [lng]) sits relative to the venue centre, x east and y south, on a plan that is
   * not turned (bearing 0).
   *
   * @param venueLat Latitude of the venue centre, in degrees.
   * @param venueLng Longitude of the venue centre, in degrees.
   * @param lat Latitude of the point, in degrees.
   * @param lng Longitude of the point, in degrees.
   * @return The metres, x east then y south.
   */
  fun metresFrom(
      venueLat: Double,
      venueLng: Double,
      lat: Double,
      lng: Double,
  ): Pair<Double, Double> = metresFrom(venueLat, venueLng, 0.0, lat, lng)

  /**
   * The four corners of a footprint at ([x], [y]) with [width] and [height], clockwise from the top
   * left, as earth points.
   *
   * @param centerLat Latitude of the venue centre, in degrees.
   * @param centerLng Longitude of the venue centre, in degrees.
   * @param bearingDegrees How far the plan is turned clockwise from north.
   * @param x Plan metres east of the centre of the footprint's top left corner.
   * @param y Plan metres south of the centre of the footprint's top left corner.
   * @param width Metres the footprint spans to the east on the plan.
   * @param height Metres the footprint spans to the south on the plan.
   * @return Top left, top right, bottom right, bottom left.
   */
  fun corners(
      centerLat: Double,
      centerLng: Double,
      bearingDegrees: Double,
      x: Double,
      y: Double,
      width: Double,
      height: Double,
  ): List<LatLngPoint> =
      listOf(x to y, x + width to y, x + width to y + height, x to y + height).map { (px, py) ->
        latLngFrom(centerLat, centerLng, bearingDegrees, px, py)
      }

  /**
   * The straight line distance between two plan points.
   *
   * @return The distance in metres.
   */
  fun distanceMetres(x1: Double, y1: Double, x2: Double, y2: Double): Double =
      hypot(x2 - x1, y2 - y1)

  /**
   * Minutes on foot at 1.2 m/s, rounded up, never below 1.
   *
   * @param metres The distance to walk.
   */
  fun walkingMinutes(metres: Double): Int =
      ceil(metres / WALKING_METRES_PER_SECOND / 60).toInt().coerceAtLeast(1)

  /**
   * Rounds to the nearest half metre, the grid structures snap to.
   *
   * @param metres The position or size to round.
   */
  fun snap(metres: Double): Double = (metres * 2).roundToLong() / 2.0

  /**
   * "6" for whole metres, "2.5" otherwise.
   *
   * @param metres The value to show.
   */
  fun metresText(metres: Double): String =
      if (metres % 1.0 == 0.0) metres.toLong().toString() else metres.toString()
}
