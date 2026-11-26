package com.android.festivar.ui.event.create

// Co-authored-by: Gemini <noreply@google.com>

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.theme.AppTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [CreateEventScreen] running on Robolectric to ensure code coverage. */
@RunWith(AndroidJUnit4::class)
class CreateEventScreenRobolectricTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val creator = User("creator-123", "Alice", "AliceInWonderland")
  private val dataRepository =
      DataRepositoryLocal(
          authRepository = mockk(),
          usersRepository = UsersRepositoryLocal(listOf(creator)),
      )

  /** Breaks if the create event screen preview content does not display core fields. */
  @Test
  fun createEventScreenPreviewShowsCoreElements() {
    composeTestRule.setContent {
      AppTheme {
        CreateEventContent(
            state = CreateEventUIState(),
            actions = CreateEventActions(),
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsDisplayed()
  }

  /** Breaks if image viewer and delete button fail to display when an image is present. */
  @Test
  fun createEventScreenDisplaysImageViewerWhenImageIsPresent() {
    val viewModel = CreateEventViewModel(dataRepository, creator.uid)
    viewModel.setImageUrlForTesting("https://example.com/image.png")
    composeTestRule.setContent { AppTheme { CreateEventScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_BUTTON).assertIsDisplayed()
  }

  /** Breaks if the create event screen does not display saving indicator when saving. */
  @Test
  fun createEventScreenShowsSavingIndicatorAndDisablesElements() {
    composeTestRule.setContent {
      AppTheme {
        CreateEventContent(
            state = CreateEventUIState(isSaving = true, name = "Summer festival"),
            actions = CreateEventActions(),
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.SAVING_INDICATOR).assertIsDisplayed()
  }

  /** Breaks if delete image confirmation dialog does not appear when requested. */
  @Test
  fun createEventScreenShowsDeleteImageConfirmationDialog() {
    val viewModel = CreateEventViewModel(dataRepository, creator.uid)
    viewModel.setImageUrlForTesting("https://example.com/image.png")
    viewModel.requestDeleteImage()
    composeTestRule.setContent { AppTheme { CreateEventScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(CreateEventScreenTestTags.DELETE_IMAGE_CONFIRM_DIALOG)
        .assertIsDisplayed()
  }

  /** Breaks if discard confirmation dialog does not appear when discard is requested. */
  @Test
  fun createEventScreenShowsDiscardConfirmationDialog() {
    val viewModel = CreateEventViewModel(dataRepository, creator.uid)
    viewModel.updateName("Festival")
    viewModel.requestDiscard()
    composeTestRule.setContent { AppTheme { CreateEventScreen(viewModel = viewModel) } }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.DISCARD_DIALOG).assertIsDisplayed()
  }

  /** Breaks if image selection button triggers the photo picker callback. */
  @Test
  fun createEventScreenTriggersPhotoPickerOnSelectImageClick() {
    val launchPhotoPicker = mockk<(String) -> Unit>(relaxed = true)
    val viewModel = CreateEventViewModel(dataRepository, creator.uid)
    composeTestRule.setContent {
      AppTheme { CreateEventScreen(viewModel = viewModel, launchPhotoPicker = launchPhotoPicker) }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.IMAGE_SELECT_BUTTON).performClick()
    verify(exactly = 1) { launchPhotoPicker("image/*") }
  }
}
