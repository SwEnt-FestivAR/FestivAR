// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.location

import kotlinx.coroutines.flow.Flow

/**
 * A [LocationSource] that replays the given [fixes], for tests and previews.
 *
 * @param fixes The fixes [updates] emits, in order.
 */
class LocationSourceLocal(private val fixes: Flow<LocationFix>) : LocationSource {
  override fun updates(): Flow<LocationFix> = fixes
}
