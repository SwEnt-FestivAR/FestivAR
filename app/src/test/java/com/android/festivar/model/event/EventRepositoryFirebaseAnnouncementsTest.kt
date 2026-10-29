// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.event.announcement.Announcement
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Announcements are not stored in Firestore yet, so the Firebase repository refuses them without
 * touching the database. The strict mock fails on any call to it.
 */
class EventRepositoryFirebaseAnnouncementsTest {
  private val db = mockk<FirebaseFirestore>()
  private val repository = EventRepositoryFirebase(db)

  private val announcement =
      Announcement(
          announcementId = "a1",
          title = "Gates open",
          authorId = "u1",
          updatedAt = ZonedDateTime.parse("2026-10-17T10:00:00+02:00"),
      )

  @Test
  fun addAnnouncement_isNotSupported() {
    assertThrows(UnsupportedOperationException::class.java) {
      runBlocking { repository.addAnnouncement("e1", announcement) }
    }
  }

  @Test
  fun editAnnouncement_isNotSupported() {
    assertThrows(UnsupportedOperationException::class.java) {
      runBlocking { repository.editAnnouncement("e1", announcement) }
    }
  }

  @Test
  fun deleteAnnouncement_isNotSupported() {
    assertThrows(UnsupportedOperationException::class.java) {
      runBlocking { repository.deleteAnnouncement("e1", announcement.announcementId) }
    }
  }
}
