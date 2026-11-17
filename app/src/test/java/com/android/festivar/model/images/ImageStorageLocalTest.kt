// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.images

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImageStorageLocalTest {

  private val storage = ImageStorageLocal()

  @Test
  fun getNewUid_returnsUniqueIds() {
    assertNotEquals(storage.getNewUid(), storage.getNewUid())
  }

  @Test fun delete_doesNotThrow() = runTest { storage.delete("file://avatar.png") }
}
