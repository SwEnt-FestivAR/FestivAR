// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StructureRepositoryFirebaseTest {
  private lateinit var repository: StructureRepositoryFirebase
  private val createdIds = mutableListOf<String>()

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    runBlocking { Firebase.auth.signInAnonymously().await() }
    repository = StructureRepositoryFirebase(Firebase.firestore)
  }

  @After
  fun tearDown() {
    runBlocking {
      createdIds.forEach {
        runCatching {
          Firebase.firestore.collection(STRUCTURE_COLLECTION_PATH).document(it).delete().await()
        }
      }
      Firebase.auth.signOut()
    }
  }

  private fun newStructure(
      eventId: String = "event-${UUID.randomUUID()}",
      kind: StructureKind = StructureKind.CUSTOM,
  ): Structure {
    val id = "structure-${UUID.randomUUID()}"
    createdIds.add(id)
    return Structure(id, eventId, kind, "Food truck", 2.5, -1.25, 3.5, 0.5)
  }

  /** Breaks if any field, the CUSTOM kind or a fractional metre is lost in Firestore. */
  @Test
  fun addThenGet_roundTripsEveryField() = runBlocking {
    val structure = newStructure()

    repository.addStructure(structure)

    assertEquals(structure, repository.getStructure(structure.structureId))
  }

  /** Breaks if a kind other than CUSTOM does not survive the enum name round trip. */
  @Test
  fun addThenGet_roundTripsEveryKind() = runBlocking {
    for (kind in StructureKind.entries) {
      val structure = newStructure(kind = kind)
      repository.addStructure(structure)
      assertEquals(structure, repository.getStructure(structure.structureId))
    }
  }

  /** Breaks if getAllStructures returns structures of another event. */
  @Test
  fun getAllStructures_returnsOnlyThatEvent() = runBlocking {
    val first = newStructure(eventId = "event-a-${UUID.randomUUID()}")
    val second = newStructure(eventId = first.eventId)
    val other = newStructure()
    listOf(first, second, other).forEach { repository.addStructure(it) }

    val result = repository.getAllStructures(first.eventId)

    assertEquals(setOf(first, second), result.toSet())
    assertEquals(2, result.size)
  }

  /** Breaks if a second add with the same id overwrites instead of refusing. */
  @Test
  fun addStructure_refusesADuplicateId() = runBlocking {
    val structure = newStructure()
    repository.addStructure(structure)

    val failure = runCatching { repository.addStructure(structure.copy(label = "Other")) }
    assertTrue(failure.exceptionOrNull() is IllegalArgumentException)
    assertEquals(structure, repository.getStructure(structure.structureId))
  }

  /** Breaks if edit keeps the old values. */
  @Test
  fun editStructure_replacesTheValue() = runBlocking {
    val structure = newStructure()
    repository.addStructure(structure)
    val edited = structure.copy(label = "Moved", x = 10.75, width = 8.0)

    repository.editStructure(structure.structureId, edited)

    assertEquals(edited, repository.getStructure(structure.structureId))
  }

  /** Breaks if edit of a missing id creates the document. */
  @Test
  fun editStructure_ofAMissingIdThrowsAndCreatesNothing() = runBlocking {
    val structure = newStructure()

    val failure = runCatching { repository.editStructure(structure.structureId, structure) }

    assertTrue(failure.exceptionOrNull() is NoSuchElementException)
    assertTrue(runCatching { repository.getStructure(structure.structureId) }.isFailure)
  }

  /** Breaks if delete leaves the document, or a second delete passes silently. */
  @Test
  fun deleteStructure_removesItAndASecondDeleteThrows() = runBlocking {
    val structure = newStructure()
    repository.addStructure(structure)

    repository.deleteStructure(structure.structureId)

    assertTrue(
        runCatching { repository.getStructure(structure.structureId) }.exceptionOrNull()
            is NoSuchElementException
    )
    assertTrue(
        runCatching { repository.deleteStructure(structure.structureId) }.exceptionOrNull()
            is NoSuchElementException
    )
  }

  /** Breaks if a document missing a required field is read as a default instead of failing. */
  @Test
  fun getStructure_throwsWhenLabelIsMissing() = runBlocking {
    val structure = newStructure()
    Firebase.firestore
        .collection(STRUCTURE_COLLECTION_PATH)
        .document(structure.structureId)
        .set(
            mapOf(
                "structureId" to structure.structureId,
                "eventId" to structure.eventId,
                "kind" to structure.kind.name,
                "x" to structure.x,
                "y" to structure.y,
                "width" to structure.width,
                "height" to structure.height,
            )
        )
        .await()

    val failure = runCatching { repository.getStructure(structure.structureId) }

    assertTrue(failure.exceptionOrNull() is IllegalStateException)
    assertTrue(failure.exceptionOrNull()!!.message!!.contains("label"))
  }
}
