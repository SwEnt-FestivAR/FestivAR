// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.geocoding

/** Turns a typed address into places. Implementations may call the network. */
interface GeocodingRepository {

  /**
   * Searches the places an address may mean.
   *
   * @param query The address as the organizer typed it.
   * @param limit The most places to return, at least 1.
   * @return Up to [limit] [GeocodingResult] items, best first. Empty when [query] is blank, when
   *   nothing matches, or when the device has no geocoding backend.
   * @throws IllegalArgumentException if [limit] is below 1.
   * @throws java.io.IOException when the backend cannot be reached.
   */
  suspend fun search(query: String, limit: Int = 5): List<GeocodingResult>
}
