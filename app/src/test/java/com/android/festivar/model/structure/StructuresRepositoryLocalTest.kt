// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class StructuresRepositoryLocalTest {
  private lateinit var repository: StructuresRepositoryLocal

  private val structure =
      Structure(
          structureId = "1",
          eventId = "event1",
          kind = StructureKind.CUSTOM,
          label = "Bar",
          x = 2.5,
          y = -3.0,
          width = 4.0,
          height = 1.5,
      )

  @Before
  fun setUp() {
    repository = StructuresRepositoryLocal()
  }

  /** Breaks if getNewUid returns an empty or repeated identifier. */
  @Test
  fun getNewUid_isNonEmptyAndUnique() {
    val first = repository.getNewUid()
    val second = repository.getNewUid()

    assertTrue(first.isNotEmpty())
    assertNotSame(first, second)
    assertTrue(first != second)
  }

  /** Breaks if an added structure cannot be read back unchanged. */
  @Test
  fun addStructure_thenGet_returnsIt() = runTest {
    repository.addStructure(structure)

    assertEquals(structure, repository.getStructure("1"))
    assertEquals(listOf(structure), repository.getAllStructures("event1"))
  }

  /** Breaks if the initial list is not served. */
  @Test
  fun initialStructures_areServed() = runTest {
    val seeded = StructuresRepositoryLocal(listOf(structure))

    assertEquals(structure, seeded.getStructure("1"))
  }

  /** Breaks if a second add with the same id overwrites or duplicates the first. */
  @Test
  fun addStructure_refusesADuplicateId() = runTest {
    repository.addStructure(structure)

    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.addStructure(structure.copy(label = "Other")) }
    }

    assertEquals(listOf(structure), repository.getAllStructures("event1"))
  }

  /** Breaks if getAllStructures leaks another event's structures. */
  @Test
  fun getAllStructures_filtersByEvent() = runTest {
    val other = structure.copy(structureId = "2", eventId = "event2")
    repository.addStructure(structure)
    repository.addStructure(other)

    assertEquals(listOf(structure), repository.getAllStructures("event1"))
    assertEquals(listOf(other), repository.getAllStructures("event2"))
    assertTrue(repository.getAllStructures("event3").isEmpty())
  }

  /** Breaks if edit appends instead of replacing, or keeps the old value. */
  @Test
  fun editStructure_replacesTheValue() = runTest {
    repository.addStructure(structure)
    val edited = structure.copy(label = "Big bar", width = 6.0)

    repository.editStructure("1", edited)

    assertEquals(listOf(edited), repository.getAllStructures("event1"))
  }

  /** Breaks if edit lets one structure overwrite another through a different id. */
  @Test
  fun editStructure_refusesADifferentId() = runTest {
    val second = structure.copy(structureId = "2")
    repository.addStructure(structure)
    repository.addStructure(second)

    assertThrows(IllegalArgumentException::class.java) {
      runBlocking { repository.editStructure("1", second.copy(label = "Overwrite")) }
    }

    assertEquals(listOf(structure, second), repository.getAllStructures("event1"))
  }

  /** Breaks if getting a missing id returns something instead of throwing. */
  @Test
  fun getStructure_throwsWhenMissing() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.getStructure("missing") }
    }
  }

  /** Breaks if editing a missing id creates it or fails with another exception. */
  @Test
  fun editStructure_throwsWhenMissing() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.editStructure("1", structure) }
    }
  }

  /** Breaks if delete does not remove the structure, or removes another one. */
  @Test
  fun deleteStructure_removesOnlyThatOne() = runTest {
    val second = structure.copy(structureId = "2")
    repository.addStructure(structure)
    repository.addStructure(second)

    repository.deleteStructure("1")

    assertEquals(listOf(second), repository.getAllStructures("event1"))
  }

  /** Breaks if deleting a missing id passes silently. */
  @Test
  fun deleteStructure_throwsWhenMissing() {
    assertThrows(NoSuchElementException::class.java) {
      runBlocking { repository.deleteStructure("missing") }
    }
  }
}
