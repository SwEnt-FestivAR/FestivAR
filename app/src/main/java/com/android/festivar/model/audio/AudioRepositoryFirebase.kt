package com.android.festivar.model.audio

import androidx.core.net.toUri
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AudioRepositoryFirebase(storageRef: StorageReference) : AudioRepository {
  val audioRef = storageRef.child("audio")

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAudio(fileName: String): Audio {
    val ref = audioRef.child(fileName)
    val localFile = withContext(IO) { File.createTempFile("audio_", ".mp3") }
    ref.getFile(localFile).await()
    val downloadUrl = ref.downloadUrl.await().toString()
    return Audio(localFile.toUri(), downloadUrl)
  }

  override suspend fun addAudio(audio: Audio) {
    val ref = audio.uri.lastPathSegment?.let { audioRef.child(it) }
    require(ref != null) { "Can't access firebase storage" }
    ref.putFile(audio.uri).await()
    audio.url = ref.downloadUrl.await().toString()
  }

  override suspend fun editAudio(
      uri: String,
      newValue: Audio,
  ) {
    deleteAudio(uri)
    addAudio(newValue)
  }

  override suspend fun deleteAudio(fileName: String) {
    val ref = audioRef.child(fileName)
    ref.delete().await()
  }
}
