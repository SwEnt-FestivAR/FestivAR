// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

/* What a walk recorded with the GPS goes through before it becomes an outline. */

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
