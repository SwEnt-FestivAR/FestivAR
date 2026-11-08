// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

import java.util.UUID

/**
 * Represents a repository that manages a local list of structures.
 *
 * @param initial The structures the repository starts with.
 */
class StructuresRepositoryLocal(initial: List<Structure> = emptyList()) : StructuresRepository {
  private val structures: MutableList<Structure> = initial.toMutableList()

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllStructures(eventId: String): List<Structure> {
    return structures.filter { it.eventId == eventId }
  }

  override suspend fun getStructure(structureId: String): Structure {
    return structures.find { it.structureId == structureId }
        ?: throw NoSuchElementException("StructuresRepositoryLocal: $structureId not found")
  }

  override suspend fun addStructure(structure: Structure) {
    require(structures.none { it.structureId == structure.structureId }) {
      "StructuresRepositoryLocal: ${structure.structureId} already exists"
    }
    structures.add(structure)
  }

  override suspend fun editStructure(structureId: String, newValue: Structure) {
    require(newValue.structureId == structureId) {
      "StructuresRepositoryLocal: newValue.structureId must be equal to $structureId"
    }

    val index = structures.indexOfFirst { it.structureId == structureId }
    if (index == -1) {
      throw NoSuchElementException("StructuresRepositoryLocal: $structureId not found")
    }
    structures[index] = newValue
  }

  override suspend fun deleteStructure(structureId: String) {
    val index = structures.indexOfFirst { it.structureId == structureId }
    if (index == -1) {
      throw NoSuchElementException("StructuresRepositoryLocal: $structureId not found")
    }
    structures.removeAt(index)
  }
}
