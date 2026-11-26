package com.android.festivar.navigation.announcement

import org.junit.Assert.assertEquals
import org.junit.Test

class AnnouncementNavGraphHelperTest {

  @Test
  fun requireEventIdReturnsValidString() {
    assertEquals("e1", requireEventId("e1"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireEventIdThrowsWhenNull() {
    requireEventId(null)
  }

  @Test
  fun requireAnnouncementArgumentsReturnsValidPair() {
    val result = requireAnnouncementArguments("e1", "a1")
    assertEquals("e1" to "a1", result)
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementArgumentsThrowsWhenEventIdNull() {
    requireAnnouncementArguments(null, "a1")
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementArgumentsThrowsWhenAnnouncementIdNull() {
    requireAnnouncementArguments("e1", null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementArgumentsThrowsWhenEventIdBlank() {
    requireAnnouncementArguments("   ", "a1")
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementArgumentsThrowsWhenAnnouncementIdBlank() {
    requireAnnouncementArguments("e1", "   ")
  }
}
