// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map

import com.android.festivar.model.map.geometry.GeoPosition

/**
 * A place with a name, as a search for an address returns it.
 *
 * Rule, checked in `init`: [name] is not blank.
 *
 * @property name What the place is called, for instance "Parc de Vidy, Lausanne, Vaud".
 * @property position Where the place is on Earth.
 */
data class Location(val name: String, val position: GeoPosition) {
  init {
    require(name.isNotBlank()) { "The name of a location cannot be blank." }
  }
}
