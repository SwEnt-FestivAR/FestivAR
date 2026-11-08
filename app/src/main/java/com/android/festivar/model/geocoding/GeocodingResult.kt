// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.geocoding

/**
 * One place an address may mean.
 *
 * @property label The place as the backend names it, for the list.
 * @property latitude The latitude in degrees, from -90 to 90.
 * @property longitude The longitude in degrees, from -180 to 180.
 * @throws IllegalArgumentException if [label] is blank or a coordinate is out of range.
 */
data class GeocodingResult(val label: String, val latitude: Double, val longitude: Double) {
  init {
    require(label.isNotBlank()) { "label must not be blank" }
    require(latitude in -90.0..90.0) { "latitude must be in -90..90, was $latitude" }
    require(longitude in -180.0..180.0) { "longitude must be in -180..180, was $longitude" }
  }
}
