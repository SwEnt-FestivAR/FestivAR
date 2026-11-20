// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

/**
 * One reading of the phone's position, as recorded while walking around a site.
 *
 * Rule, checked in `init`: [accuracyMeters] is finite and not negative.
 *
 * @property position Where the phone says it is.
 * @property accuracyMeters The radius, in metres, the real position is expected to be within. The
 *   smaller, the better the reading.
 */
data class GpsSample(val position: GeoPosition, val accuracyMeters: Double) {
  init {
    require(accuracyMeters.isFinite() && accuracyMeters >= 0.0) {
      "The accuracy must be finite and not negative."
    }
  }
}
