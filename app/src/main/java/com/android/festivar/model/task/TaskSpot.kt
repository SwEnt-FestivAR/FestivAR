// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.task

/**
 * Where a [Task] sits on the venue plan: on a structure, or at a free point. Rules: exactly one of
 * [structureId] and the pair ([x], [y]) is set; a structureId, when set, is not blank.
 *
 * @property structureId The structure the task is pinned to, or null for a free point.
 * @property x Metres east of the venue centre of a free point, null when pinned to a structure.
 * @property y Metres south of the venue centre of a free point, null when pinned to a structure.
 */
data class TaskSpot(val structureId: String? = null, val x: Double? = null, val y: Double? = null) {
  init {
    require((structureId != null) != (x != null && y != null)) {
      "A spot is a structure or a point, not both and not neither."
    }
    require(structureId == null || structureId.isNotBlank()) { "The structureId cannot be blank." }
    require((x == null) == (y == null)) { "x and y go together." }
  }

  companion object {
    /** A spot on the structure [structureId]. */
    fun onStructure(structureId: String) = TaskSpot(structureId = structureId)

    /** A free point [x] metres east and [y] metres south of the venue centre. */
    fun at(x: Double, y: Double) = TaskSpot(x = x, y = y)
  }
}
