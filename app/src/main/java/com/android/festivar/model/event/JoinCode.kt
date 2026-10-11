// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

/** Every join code is this long. */
const val CODE_LENGTH = 6

/** The code an organizer shares: the first six letters or digits of the event id, uppercased. */
fun joinCodeOf(event: Event): String =
    event.eventId.filter { it.isLetterOrDigit() }.take(CODE_LENGTH).uppercase()
