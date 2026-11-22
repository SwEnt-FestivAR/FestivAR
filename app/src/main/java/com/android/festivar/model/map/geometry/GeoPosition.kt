// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
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

  /** @return The position as stored in a document, by the names of [LATITUDE] and [LONGITUDE]. */
  fun toMap(): Map<String, Double> = mapOf(LATITUDE to latitude, LONGITUDE to longitude)

  companion object {
    private const val LATITUDE = "latitude"
    private const val LONGITUDE = "longitude"

    /**
     * Reads a position as [toMap] stores it.
     *
     * @param map The stored position.
     * @return The [GeoPosition] of [map].
     * @throws IllegalArgumentException If a coordinate is missing, is not a number, or breaks the
     *   rules of [GeoPosition].
     */
    fun fromMap(map: Map<*, *>): GeoPosition {
      val latitude = requireNotNull(map[LATITUDE] as? Number) { "The latitude must be a number." }
      val longitude =
          requireNotNull(map[LONGITUDE] as? Number) { "The longitude must be a number." }
      return GeoPosition(latitude.toDouble(), longitude.toDouble())
    }
  }
}
