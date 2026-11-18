// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map

/** Finds places from what a user typed. */
interface LocationRepository {

  /**
   * The places that match [query], the most relevant first. The list is empty when nothing matches
   * or when [query] is blank.
   *
   * @throws java.io.IOException if the search service cannot be reached or answers with an error.
   */
  suspend fun search(query: String): List<Location>
}
