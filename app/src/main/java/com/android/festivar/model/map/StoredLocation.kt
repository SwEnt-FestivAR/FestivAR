package com.android.festivar.model.map

// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

/**
 * Reads the location field of a stored document, for every document that has one (an event, a
 * task), so that they all read it the same way.
 *
 * A document saved before locations had coordinates stored its location as a text, which cannot
 * give a [Location]. It reads as no location, like a document saved without one, and the text is
 * not kept. The handling of old texts can be removed once the stored texts are set to `null`
 * (#756).
 *
 * @param stored The value of the location field, as the document returns it.
 * @param owner What the document is, for the message of an invalid location, for instance "event
 *   abc".
 * @return The stored [Location], or `null` when there is none.
 * @throws IllegalArgumentException If [stored] is neither absent, nor a text, nor a valid location.
 */
internal fun readStoredLocation(stored: Any?, owner: String): Location? {
  if (stored is String) return null
  return try {
    Location.fromStoredOrNull(stored)
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException("Invalid location data for $owner.", e)
  }
}
