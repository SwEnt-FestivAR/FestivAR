// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * The geometry every map feature relies on, in plain Kotlin: no Android, Firebase or Google Maps
 * type appears here.
 *
 * Conventions:
 * - a site has one origin, a GeoPosition; everything else is a LocalPosition in metres, x to the
 *   east and y to the north of that origin;
 * - a rotation is in degrees, clockwise from north, as on a compass;
 * - a polygon is a list of corners that is not closed: the first corner is not repeated at the end.
 */

/** The radius of the Earth taken as a sphere, in metres. */
const val EARTH_RADIUS_METERS = 6_371_000.0

/** Below this, in metres or square metres, two values are taken as equal. */
private const val EPSILON = 1e-9

/**
 * Where [position] is in metres from [origin].
 *
 * The Earth is taken as flat around the origin (an equirectangular projection). At the scale of a
 * site, a few kilometres, the error is a few centimetres per kilometre. It is not meant for an
 * origin close to a pole, nor for a site that crosses the antimeridian (longitude 180).
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

/** The area of [polygon] in square metres; 0 with fewer than three corners. */
fun polygonArea(polygon: List<LocalPosition>): Double = abs(signedArea(polygon))

/**
 * The centre of mass of [polygon]. When the polygon has no area (fewer than three corners, or all
 * of them on one line) it is the average of the corners.
 *
 * @throws IllegalArgumentException if [polygon] is empty.
 */
fun polygonCentroid(polygon: List<LocalPosition>): LocalPosition {
  require(polygon.isNotEmpty()) { "An empty polygon has no centroid." }
  val area = signedArea(polygon)
  if (abs(area) < EPSILON) {
    return LocalPosition(
        polygon.sumOf { it.x } / polygon.size,
        polygon.sumOf { it.y } / polygon.size,
    )
  }
  var x = 0.0
  var y = 0.0
  forEachEdge(polygon) { a, b ->
    val cross = a.x * b.y - b.x * a.y
    x += (a.x + b.x) * cross
    y += (a.y + b.y) * cross
  }
  return LocalPosition(x / (6 * area), y / (6 * area))
}

/**
 * Whether [point] is inside [polygon]. A point on an edge or on a corner is inside. Always false
 * with fewer than three corners.
 */
fun polygonContains(polygon: List<LocalPosition>, point: LocalPosition): Boolean {
  if (polygon.size < 3) return false
  var inside = false
  forEachEdge(polygon) { a, b ->
    if (isOnSegment(a, b, point)) return true
    // Ray casting: count the edges crossed by the half line going east from the point.
    if ((a.y > point.y) != (b.y > point.y)) {
      val xOfEdge = a.x + (point.y - a.y) / (b.y - a.y) * (b.x - a.x)
      if (point.x < xOfEdge) inside = !inside
    }
  }
  return inside
}

/**
 * Whether [polygon] is a real outline: at least three corners, no two consecutive corners at the
 * same place, and no edge that touches or crosses another one beyond the corner they share.
 */
fun isSimplePolygon(polygon: List<LocalPosition>): Boolean {
  val size = polygon.size
  if (size < 3) return false
  for (i in 0 until size) {
    val a = polygon[i]
    val b = polygon[(i + 1) % size]
    val c = polygon[(i + 2) % size]
    if (distanceMeters(a, b) < EPSILON) return false
    // The next edge starts at b: it must not come back over this one.
    val foldsBack = (a.x - b.x) * (c.x - b.x) + (a.y - b.y) * (c.y - b.y) > 0
    if (abs(cross(a, b, c)) < EPSILON && foldsBack) return false
    for (j in i + 2 until size) {
      if (i == 0 && j == size - 1) continue // the last edge shares a corner with the first
      if (segmentsTouch(a, b, polygon[j], polygon[(j + 1) % size])) return false
    }
  }
  return true
}

/**
 * The four corners of a rectangle centred on [center], [width] metres wide and [depth] metres deep,
 * turned by [rotationDegrees] clockwise from north.
 *
 * Without rotation the width runs west to east and the depth south to north, and the corners come
 * in the order north west, north east, south east, south west. A rotation turns the rectangle
 * around its centre and keeps that order.
 *
 * @throws IllegalArgumentException if [width] or [depth] is not finite and positive, or
 *   [rotationDegrees] is not finite.
 */
fun rectangleCorners(
    center: LocalPosition,
    width: Double,
    depth: Double,
    rotationDegrees: Double,
): List<LocalPosition> {
  require(width.isFinite() && width > 0.0) { "The width must be finite and positive." }
  require(depth.isFinite() && depth > 0.0) { "The depth must be finite and positive." }
  require(rotationDegrees.isFinite()) { "The rotation must be finite." }
  val angle = Math.toRadians(rotationDegrees)
  val halfWidth = width / 2
  val halfDepth = depth / 2
  return listOf(
          LocalPosition(-halfWidth, halfDepth),
          LocalPosition(halfWidth, halfDepth),
          LocalPosition(halfWidth, -halfDepth),
          LocalPosition(-halfWidth, -halfDepth),
      )
      .map { corner ->
        // Clockwise seen from above: north goes to east for a quarter turn.
        LocalPosition(
            center.x + corner.x * cos(angle) + corner.y * sin(angle),
            center.y - corner.x * sin(angle) + corner.y * cos(angle),
        )
      }
}

/**
 * [points] with the ones that add no shape removed (Douglas-Peucker): every removed point was
 * within [toleranceMeters] of the simplified path. The first and the last point are always kept,
 * and the order is unchanged.
 *
 * @throws IllegalArgumentException if [toleranceMeters] is negative or not a number.
 */
fun simplifyPath(points: List<LocalPosition>, toleranceMeters: Double): List<LocalPosition> {
  require(toleranceMeters >= 0.0) { "The tolerance cannot be negative." }
  if (points.size < 3) return points
  val kept = BooleanArray(points.size)
  kept[0] = true
  kept[points.lastIndex] = true
  // A list of stretches still to look at, in place of a recursion a long path would overflow.
  val stretches = ArrayDeque(listOf(0 to points.lastIndex))
  while (stretches.isNotEmpty()) {
    val (first, last) = stretches.removeLast()
    var farthest = -1
    var farthestDistance = toleranceMeters
    for (i in first + 1 until last) {
      val distance = distanceToSegment(points[i], points[first], points[last])
      if (distance > farthestDistance) {
        farthest = i
        farthestDistance = distance
      }
    }
    if (farthest != -1) {
      kept[farthest] = true
      stretches.addLast(first to farthest)
      stretches.addLast(farthest to last)
    }
  }
  return points.filterIndexed { index, _ -> kept[index] }
}

/**
 * The [samples] worth keeping from a recorded walk, in order: a sample is dropped when its accuracy
 * is worse than [maxAccuracyMeters], or when it is closer than [minDistanceMeters] to the last
 * sample kept.
 *
 * @throws IllegalArgumentException if a limit is negative or not a number.
 */
fun filterGpsSamples(
    samples: List<GpsSample>,
    maxAccuracyMeters: Double,
    minDistanceMeters: Double,
): List<GpsSample> {
  require(maxAccuracyMeters >= 0.0) { "The maximum accuracy cannot be negative." }
  require(minDistanceMeters >= 0.0) { "The minimum distance cannot be negative." }
  val kept = mutableListOf<GpsSample>()
  for (sample in samples) {
    if (sample.accuracyMeters > maxAccuracyMeters) continue
    val previous = kept.lastOrNull()
    if (
        previous == null || distanceMeters(previous.position, sample.position) >= minDistanceMeters
    ) {
      kept += sample
    }
  }
  return kept
}

/** Calls [action] on each edge of [polygon], the one closing it included. */
private inline fun forEachEdge(
    polygon: List<LocalPosition>,
    action: (LocalPosition, LocalPosition) -> Unit,
) {
  for (i in polygon.indices) action(polygon[i], polygon[(i + 1) % polygon.size])
}

/** The area of [polygon], positive when its corners turn counterclockwise (shoelace formula). */
private fun signedArea(polygon: List<LocalPosition>): Double {
  if (polygon.size < 3) return 0.0
  var sum = 0.0
  forEachEdge(polygon) { a, b -> sum += a.x * b.y - b.x * a.y }
  return sum / 2
}

/** Positive when [c] is to the left of the line from [a] to [b], zero when it is on it. */
private fun cross(a: LocalPosition, b: LocalPosition, c: LocalPosition): Double =
    (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

/** Whether [point] is on the segment from [a] to [b], ends included. */
private fun isOnSegment(a: LocalPosition, b: LocalPosition, point: LocalPosition): Boolean =
    distanceToSegment(point, a, b) < EPSILON

/** Whether the segments [a] to [b] and [c] to [d] have at least one point in common. */
private fun segmentsTouch(
    a: LocalPosition,
    b: LocalPosition,
    c: LocalPosition,
    d: LocalPosition,
): Boolean {
  if (
      isOnSegment(c, d, a) || isOnSegment(c, d, b) || isOnSegment(a, b, c) || isOnSegment(a, b, d)
  ) {
    return true
  }
  // No end on the other segment: they cross when each one has its ends on both sides of the other.
  return (cross(c, d, a) > 0) != (cross(c, d, b) > 0) &&
      (cross(a, b, c) > 0) != (cross(a, b, d) > 0)
}

/** The distance in metres from [point] to the closest point of the segment from [a] to [b]. */
private fun distanceToSegment(point: LocalPosition, a: LocalPosition, b: LocalPosition): Double {
  val lengthSquared = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
  if (lengthSquared == 0.0) return distanceMeters(point, a)
  val along =
      (((point.x - a.x) * (b.x - a.x) + (point.y - a.y) * (b.y - a.y)) / lengthSquared).coerceIn(
          0.0,
          1.0,
      )
  return hypot(point.x - (a.x + along * (b.x - a.x)), point.y - (a.y + along * (b.y - a.y)))
}
