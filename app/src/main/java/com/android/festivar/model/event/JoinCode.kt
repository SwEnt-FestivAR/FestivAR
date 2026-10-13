// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

/**
 * A join code is at most this long. An id with fewer than six letters and digits yields a shorter
 * code, which the six character join form can never match; such events are only joinable once ids
 * carry a code.
 */
const val JOIN_CODE_LENGTH = 6

/**
 * The code an organizer shares: the first six letters or digits of the event id, uppercased. Two
 * events may share a code; the join screen then lists both and the person picks one.
 */
fun joinCodeOf(event: Event): String =
    event.eventId.filter { it.isLetterOrDigit() }.take(JOIN_CODE_LENGTH).uppercase()
