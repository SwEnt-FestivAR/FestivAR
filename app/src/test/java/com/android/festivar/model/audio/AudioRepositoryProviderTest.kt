// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests [AudioRepositoryProvider] and [AudioRepositoryLocal]. */
class AudioRepositoryProviderTest {
  @After
  fun tearDown() {
    AudioRepositoryProvider.reset()
  }

  /** Breaks if an assigned repository is not the one the provider hands out. */
  @Test
  fun repository_assigned_isTheOneProvided() {
    val local = AudioRepositoryLocal()

    AudioRepositoryProvider.repository = local

    assertSame(local, AudioRepositoryProvider.repository)
  }

  /** Breaks if deleting from the local repository fails or needs anything stored. */
  @Test
  fun local_delete_doesNothing() = runTest {
    AudioRepositoryLocal().delete("https://fake/t1/1.m4a")
  }

  /** Breaks if two uploads of the local repository can return the same URL. */
  @Test
  fun local_upload_returnsADifferentUrlEachTime() = runTest {
    val repository = AudioRepositoryLocal()
    val audio = io.mockk.mockk<Audio>()

    assertTrue(repository.upload(audio, "t1") != repository.upload(audio, "t1"))
  }
}
