// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.location

import kotlinx.coroutines.flow.Flow

/**
 * One position fix of the device.
 *
 * Named [LocationFix] rather than GeoPoint so it never clashes with Firestore's GeoPoint.
 *
 * @property latitude Degrees north of the equator, negative to the south.
 * @property longitude Degrees east of Greenwich, negative to the west.
 * @property accuracyMetres The radius, in metres, the fix is good to.
 */
data class LocationFix(val latitude: Double, val longitude: Double, val accuracyMetres: Float)

/** Streams the device's position. ViewModels depend on this, never on Play services. */
interface LocationSource {
  /** Fixes as they arrive; completes without a value when location is not allowed. */
  fun updates(): Flow<LocationFix>
}

/** Holds the app's [LocationSource]. */
object LocationSourceProvider {
  /** The source in use; null until MainActivity sets it, which screens read as no GPS. */
  @Volatile var source: LocationSource? = null
}
