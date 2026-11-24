// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import java.util.UUID

/**
 * An [AudioRepository] that keeps nothing, meant for tests and previews: an upload gives back a
 * made-up URL and a deletion does nothing.
 */
class AudioRepositoryLocal : AudioRepository {
  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun upload(audio: Audio, taskId: String): String =
      "local://audio/$taskId/${getNewUid()}"

  override suspend fun delete(audioUrl: String) = Unit
}
