// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.temporary

import com.android.festivar.model.event.Event
import com.android.festivar.model.task.Task

/**
 * Temporary data class representing a [User] of the FestivAR application. The implementation and
 * parameters of this class are up to changes and this definition states only as a preliminary
 * definition to allow the implementation of other classes.
 *
 * An [Event] and a [Task] identify a [User] by its [uid], whatever its [name] or [surname].
 *
 * @param uid The [User]'s unique identifier.
 * @param name The [User]'s name.
 * @param surname The [User]'s surname.
 */
data class User(val uid: String, val name: String = "", val surname: String = "")
