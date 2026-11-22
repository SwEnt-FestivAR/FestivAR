// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

/** The EPFL campus, where the shared map looks when it has no other location to show. */
val EPFL_LOCATION = GeoPosition(latitude = 46.5191, longitude = 6.5668)

/**
 * The barycenter of [locations], where the camera of the map is put: the mean of the latitudes and
 * the mean of the longitudes. With one location it is that location, and with none it is
 * [EPFL_LOCATION].
 *
 * A plain mean is enough at the scale of an event, but it is wrong for locations on both sides of
 * the 180th meridian: the mean of longitudes 179 and -179 is 0, not 180.
 */
fun barycenter(locations: List<GeoPosition>): GeoPosition {
  if (locations.isEmpty()) return EPFL_LOCATION
  return GeoPosition(
      latitude = locations.map { it.latitude }.average(),
      longitude = locations.map { it.longitude }.average(),
  )
}
