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
  fun requireAnnouncementDetailsArgumentsReturnsValidPair() {
    val result = requireAnnouncementDetailsArguments("e1", "a1")
    assertEquals("e1" to "a1", result)
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementDetailsArgumentsThrowsWhenEventIdNull() {
    requireAnnouncementDetailsArguments(null, "a1")
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementDetailsArgumentsThrowsWhenAnnouncementIdNull() {
    requireAnnouncementDetailsArguments("e1", null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementDetailsArgumentsThrowsWhenEventIdBlank() {
    requireAnnouncementDetailsArguments("   ", "a1")
  }

  @Test(expected = IllegalArgumentException::class)
  fun requireAnnouncementDetailsArgumentsThrowsWhenAnnouncementIdBlank() {
    requireAnnouncementDetailsArguments("e1", "   ")
  }
}
