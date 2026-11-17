// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.images

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageStorageLocalTest {

  @Test
  fun upload_returnsImageUri() = runTest {
    val file = File.createTempFile("avatar", ".png").apply { deleteOnExit() }
    val image = StoredImage(file)

    assertEquals(image.uri.toString(), ImageStorageLocal().upload(image, "pfp"))
  }
}
