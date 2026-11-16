// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Polygons and rectangles on the ground of a site. A polygon is a list of corners that is not
 * closed: the first corner is not repeated at the end.
 */

/** Two places closer than this, a micrometre, are taken as the same place. */
private const val TOLERANCE_METERS = 1e-6

/**
 * Below this, relative to the lengths involved, an area or a turn is taken as none. Rounding leaves
 * about a billionth of a square metre on corners a few kilometres from the origin, so a fixed
 * threshold would miss them.
 */
private const val RELATIVE_TOLERANCE = 1e-9

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
  if (abs(area) <= RELATIVE_TOLERANCE * squaredEdgeLengths(polygon)) {
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
    if (distanceMeters(a, b) < TOLERANCE_METERS) return false
    // The next edge starts at b: it must not come back over this one.
    val foldsBack = (a.x - b.x) * (c.x - b.x) + (a.y - b.y) * (c.y - b.y) > 0
    val onOneLine =
        abs(cross(a, b, c)) <= RELATIVE_TOLERANCE * distanceMeters(a, b) * distanceMeters(b, c)
    if (onOneLine && foldsBack) return false
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

/** The sum of the squared lengths of the edges of [polygon]: the scale its area is judged at. */
private fun squaredEdgeLengths(polygon: List<LocalPosition>): Double {
  var sum = 0.0
  forEachEdge(polygon) { a, b -> sum += (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y) }
  return sum
}

/** Positive when [c] is to the left of the line from [a] to [b], zero when it is on it. */
private fun cross(a: LocalPosition, b: LocalPosition, c: LocalPosition): Double =
    (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

/** Whether [point] is on the segment from [a] to [b], ends included. */
private fun isOnSegment(a: LocalPosition, b: LocalPosition, point: LocalPosition): Boolean =
    distanceToSegment(point, a, b) < TOLERANCE_METERS

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
