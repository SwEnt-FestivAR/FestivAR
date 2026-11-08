// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A [LocationSource] backed by the fused location provider of Play services, asking for a fix every
 * 2 seconds at high accuracy.
 *
 * @param context Any context; the provider client keeps the application context.
 */
class FusedLocationSource(context: Context) : LocationSource {
  private val client = LocationServices.getFusedLocationProviderClient(context)

  // The screens ask for the location permission before collecting; without it the request throws
  // a SecurityException and the flow simply ends, so there is nothing for lint to guard.
  @SuppressLint("MissingPermission")
  override fun updates(): Flow<LocationFix> = callbackFlow {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MILLIS).build()
    val callback =
        object : LocationCallback() {
          override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
              trySend(LocationFix(it.latitude, it.longitude, it.accuracy))
            }
          }
        }
    try {
      client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (_: SecurityException) {
      close()
    }
    awaitClose { client.removeLocationUpdates(callback) }
  }

  private companion object {
    const val INTERVAL_MILLIS = 2_000L
  }
}
