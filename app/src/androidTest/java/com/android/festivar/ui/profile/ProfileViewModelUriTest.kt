// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.images.ImageStorageLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileViewModelUriTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Breaks if URI selection stops copying and saving the selected profile picture.
  @Test
  fun updateProfilePictureFromUriCopiesImageAndSavesIt() {
    val user = User(uid = "u1", name = "Sara", surname = "Keller")
    val resolver = mockk<ContentResolver>()
    val uri = mockk<Uri>()
    every { resolver.getType(uri) } returns "image/png"
    every { resolver.openInputStream(uri) } returns ByteArrayInputStream("image data".toByteArray())
    val viewModel =
        ProfileViewModel(
            dataRepository =
                DataRepositoryLocal(
                    authRepository = FakeAuthRepository(),
                    usersRepository = UsersRepositoryLocal(listOf(user)),
                    imageStorage = ImageStorageLocal(),
                ),
            uId = user.uid,
        )
    composeTestRule.setContent {}
    composeTestRule.waitUntil { viewModel.uiState.value.user != null }

    val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    viewModel.updateProfilePicture(uri, resolver, cacheDir)
    composeTestRule.waitUntil { !viewModel.uiState.value.isSaving }

    assertNotNull(viewModel.uiState.value.user?.profilePictureUrl)
    assertFalse(viewModel.uiState.value.user?.profilePictureUrl.isNullOrBlank())
    assertNull(viewModel.uiState.value.errorMsg)
  }
}
