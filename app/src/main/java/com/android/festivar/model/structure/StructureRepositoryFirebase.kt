// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val STRUCTURE_COLLECTION_PATH = "structures"

/**
 * A [StructuresRepository] stored in the Firestore collection [STRUCTURE_COLLECTION_PATH].
 *
 * @param db The Firestore database to read and write.
 */
class StructureRepositoryFirebase(
    private val db: FirebaseFirestore,
) : StructuresRepository {

  private val structureCollection
    get() = db.collection(STRUCTURE_COLLECTION_PATH)

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllStructures(eventId: String): List<Structure> {
    return structureCollection.whereEqualTo("eventId", eventId).get().await().documents.map {
      documentToStructure(it)
    }
  }

  override suspend fun getStructure(structureId: String): Structure {
    val document = structureCollection.document(structureId).get().await()
    if (!document.exists()) {
      throw NoSuchElementException("Structure '$structureId' does not exist.")
    }
    return documentToStructure(document)
  }

  override suspend fun addStructure(structure: Structure) {
    val document = structureCollection.document(structure.structureId)
    db.runTransaction { transaction ->
          require(!transaction.get(document).exists()) {
            "A Structure with structureId '${structure.structureId}' already exists."
          }
          transaction.set(document, toFirestoreData(structure))
        }
        .await()
  }

  override suspend fun editStructure(structureId: String, newValue: Structure) {
    require(structureId == newValue.structureId) {
      "The edited Structure must keep its existing structureId."
    }
    val document = structureCollection.document(structureId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Structure '$structureId' does not exist.")
          }

          transaction.update(document, toFirestoreData(newValue))
        }
        .await()
  }

  override suspend fun deleteStructure(structureId: String) {
    val document = structureCollection.document(structureId)
    db.runTransaction { transaction ->
          if (!transaction.get(document).exists()) {
            throw NoSuchElementException("Structure '$structureId' does not exist.")
          }

          transaction.delete(document)
        }
        .await()
  }

  private fun toFirestoreData(structure: Structure): Map<String, Any?> {
    return mapOf(
        "structureId" to structure.structureId,
        "eventId" to structure.eventId,
        "kind" to structure.kind.name,
        "label" to structure.label,
        "x" to structure.x,
        "y" to structure.y,
        "width" to structure.width,
        "height" to structure.height,
    )
  }

  private fun documentToStructure(document: DocumentSnapshot): Structure {
    check(document.exists()) { "Structure '${document.id}' does not exist." }

    return Structure(
        structureId = document.getString("structureId") ?: document.id,
        eventId = document.getString("eventId").required("eventId", document.id),
        kind = StructureKind.valueOf(document.getString("kind").required("kind", document.id)),
        label = document.getString("label").required("label", document.id),
        x = document.getDouble("x").required("x", document.id),
        y = document.getDouble("y").required("y", document.id),
        width = document.getDouble("width").required("width", document.id),
        height = document.getDouble("height").required("height", document.id),
    )
  }

  private fun <T> T?.required(field: String, documentId: String): T {
    return this ?: error("Structure '$documentId' has missing $field")
  }
}
