// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

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
const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Where [position] is in metres from [origin].
 *
 * The Earth is taken as flat around the origin (an equirectangular projection). The error grows
 * with the square of the distance: at the latitude of Lausanne it is about 3 cm at 1 km from the
 * origin and under a metre at 5 km. It is not meant for an origin close to a pole, nor for a site
 * that crosses the antimeridian (longitude 180).
 */
fun toLocal(origin: GeoPosition, position: GeoPosition): LocalPosition {
  val x =
      Math.toRadians(position.longitude - origin.longitude) *
          cos(Math.toRadians(origin.latitude)) *
          EARTH_RADIUS_METERS
  val y = Math.toRadians(position.latitude - origin.latitude) * EARTH_RADIUS_METERS
  return LocalPosition(x, y)
}

/**
 * The position on Earth of [position], given in metres from [origin]. The exact inverse of
 * [toLocal], with the same limits.
 *
 * @throws IllegalArgumentException if the result is not on Earth, which only happens far outside
 *   those limits.
 */
fun toGeo(origin: GeoPosition, position: LocalPosition): GeoPosition {
  val latitude = origin.latitude + Math.toDegrees(position.y / EARTH_RADIUS_METERS)
  val longitude =
      origin.longitude +
          Math.toDegrees(position.x / (EARTH_RADIUS_METERS * cos(Math.toRadians(origin.latitude))))
  return GeoPosition(latitude, longitude)
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

/** [degrees] brought back within 0 (included) and 360 (excluded). */
fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

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
