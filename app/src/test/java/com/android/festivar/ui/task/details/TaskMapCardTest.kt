// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.details

import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.components.map.MapPinKind
import com.android.festivar.ui.components.map.cameraPositionFor
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskMapCardTest {
  /** Breaks if the task map draws no pin, a second pin, or a pin that is not a task pin. */
  @Test
  fun taskMapPins_isOneTaskPin() {
    val pins = taskMapPins(EPFL_LOCATION)

    assertEquals(1, pins.size)
    assertEquals(MapPinKind.TASK, pins.single().kind)
  }

  /** Breaks if the task pin is not on the location the card is given. */
  @Test
  fun taskMapPins_putsThePinOnTheLocation() {
    assertEquals(EPFL_LOCATION, taskMapPins(EPFL_LOCATION).single().position)
  }

  /** Breaks if the pin follows a hardcoded point and not the location the card is given. */
  @Test
  fun taskMapPins_followsAnotherLocation() {
    val elsewhere = GeoPosition(latitude = 46.2, longitude = 6.1)

    assertEquals(elsewhere, taskMapPins(elsewhere).single().position)
  }

  /** Breaks if the camera of the task card is not on the point where the task pin stands. */
  @Test
  fun taskMapCamera_isOnThePin() {
    val pin = taskMapPins(EPFL_LOCATION).single().position

    val target = cameraPositionFor(taskMapPins(EPFL_LOCATION).map { it.position }).target

    assertEquals(pin.latitude, target.latitude, 0.0)
    assertEquals(pin.longitude, target.longitude, 0.0)
  }
}
