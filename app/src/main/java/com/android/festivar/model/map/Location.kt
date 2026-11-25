// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map

import com.android.festivar.model.map.geometry.GeoPosition

/**
 * A place with a name, as a search for an address returns it.
 *
 * Rule, checked in `init`: [name] is not blank.
 *
 * @property name What the place is called, for instance "Parc de Vidy, Lausanne, Vaud".
 * @property position Where the place is on Earth.
 */
data class Location(val name: String, val position: GeoPosition) {
  init {
    require(name.isNotBlank()) { "The name of a location cannot be blank." }
  }

  /** @return The location as stored in a document, by the names "name" and "position". */
  fun toMap(): Map<String, Any> = mapOf(NAME to name, POSITION to position.toMap())

  companion object {
    private const val NAME = "name"
    private const val POSITION = "position"

    /**
     * Reads a location as [toMap] stores it.
     *
     * @param map The stored location.
     * @return The [Location] of [map].
     * @throws IllegalArgumentException If the name or the position is missing or invalid.
     */
    fun fromMap(map: Map<*, *>): Location {
      val name = requireNotNull(map[NAME] as? String) { "The name must be a text." }
      val position = requireNotNull(map[POSITION] as? Map<*, *>) { "The position must be a map." }
      return Location(name, GeoPosition.fromMap(position))
    }

    /**
     * Reads the value of an optional document field, so that a document saved before the field
     * existed still loads.
     *
     * @param stored The value of the field, as the document returns it.
     * @return The stored [Location], or `null` when the field is absent or `null`.
     * @throws IllegalArgumentException If [stored] is not a valid location.
     */
    fun fromStoredOrNull(stored: Any?): Location? {
      if (stored == null) return null
      require(stored is Map<*, *>) { "A stored location must be a map." }
      return fromMap(stored)
    }
  }
}
