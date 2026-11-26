// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * The coordinates every map feature relies on, in plain Kotlin: no Android, Firebase or Google
 * Maps type appears here.
 *
 * A site has one origin, a GeoPosition; everything else is a LocalPosition in metres, x to the
 * east and y to the north of that origin. A rotation is in degrees, clockwise from north, as on a
 * compass.
 */

/** The radius of the Earth taken as a sphere, in metres. */
private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Where [position] is in metres from [origin].
 *
 * The Earth is taken as flat around the origin (an equirectangular projection). The error grows
 * with the square of the distance: at the latitude of Lausanne it is about 3 cm at 1 km from the
 * origin and under a metre at 5 km. It is not meant for an origin close to a pole. A site may lie
 * across the antimeridian (longitude 180): the difference of longitude is taken the short way
 * round.
 */
fun toLocal(origin: GeoPosition, position: GeoPosition): LocalPosition {
  val x =
      Math.toRadians(wrapLongitude(position.longitude - origin.longitude)) *
          cos(Math.toRadians(origin.latitude)) *
          EARTH_RADIUS_METERS
  val y = Math.toRadians(position.latitude - origin.latitude) * EARTH_RADIUS_METERS
  return LocalPosition(x, y)
}

/**
 * The position on Earth of [position], given in metres from [origin]. The exact inverse of
 * [toLocal], with the same limits.
 *
 * @throws IllegalArgumentException if the result cannot be represented as a [GeoPosition]: past a
 *   pole, where the latitude goes beyond 90 degrees, or more than half a turn of longitude from the
 *   origin. Both only happen far outside those limits.
 */
fun toGeo(origin: GeoPosition, position: LocalPosition): GeoPosition {
  val latitude = origin.latitude + Math.toDegrees(position.y / EARTH_RADIUS_METERS)
  val longitudeOffset =
      Math.toDegrees(position.x / (EARTH_RADIUS_METERS * cos(Math.toRadians(origin.latitude))))
  require(abs(longitudeOffset) <= 180.0) { "The position is too far from the origin." }
  return GeoPosition(latitude, wrapLongitude(origin.longitude + longitudeOffset))
}

/** The distance in metres between [a] and [b] along the surface of the Earth (haversine). */
fun distanceMeters(a: GeoPosition, b: GeoPosition): Double {
  val halfLatitude = Math.toRadians(b.latitude - a.latitude) / 2
  val halfLongitude = Math.toRadians(b.longitude - a.longitude) / 2
  val h =
      sin(halfLatitude) * sin(halfLatitude) +
          cos(Math.toRadians(a.latitude)) *
              cos(Math.toRadians(b.latitude)) *
              sin(halfLongitude) *
              sin(halfLongitude)
  return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceAtMost(1.0)))
}

/** The distance in metres between [a] and [b]. */
fun distanceMeters(a: LocalPosition, b: LocalPosition): Double = hypot(b.x - a.x, b.y - a.y)

/**
 * [degrees] brought back within 0 (included) and 360 (excluded).
 *
 * @throws IllegalArgumentException if [degrees] is not finite: an angle that is not a number would
 *   otherwise travel on as NaN and fail far from its cause.
 */
fun normalizeDegrees(degrees: Double): Double {
  require(degrees.isFinite()) { "The angle must be finite." }
  return ((degrees % 360.0) + 360.0) % 360.0
}

/** A longitude, or a difference of longitudes, brought back within -180 (included) and 180. */
private fun wrapLongitude(degrees: Double): Double =
    // A value already in range is returned untouched, so an ordinary site loses no precision.
    if (degrees in -180.0..180.0) degrees else normalizeDegrees(degrees + 180.0) - 180.0

/**
 * The distance in metres from [point] to the closest point of the segment from [a] to [b], which is
 * one of its ends when the point is not level with the segment. A segment whose ends are at the
 * same place is taken as that place.
 */
fun distanceToSegment(point: LocalPosition, a: LocalPosition, b: LocalPosition): Double {
  val lengthSquared = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
  if (lengthSquared == 0.0) return distanceMeters(point, a)
  val along =
      (((point.x - a.x) * (b.x - a.x) + (point.y - a.y) * (b.y - a.y)) / lengthSquared).coerceIn(
          0.0,
          1.0,
      )
  return hypot(point.x - (a.x + along * (b.x - a.x)), point.y - (a.y + along * (b.y - a.y)))
}
