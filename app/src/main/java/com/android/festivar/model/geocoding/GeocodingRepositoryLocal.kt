// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.geocoding

/**
 * In-memory [GeocodingRepository] for tests and previews: a query matches a key when it contains
 * it, ignoring case.
 *
 * @property places The places to answer with, by key.
 */
class GeocodingRepositoryLocal(
    private val places: Map<String, List<GeocodingResult>> = emptyMap()
) : GeocodingRepository {

  override suspend fun search(query: String, limit: Int): List<GeocodingResult> {
    require(limit >= 1) { "limit must be at least 1, was $limit" }
    if (query.isBlank()) return emptyList()
    return places.filterKeys { query.contains(it, ignoreCase = true) }.values.flatten().take(limit)
  }
}
