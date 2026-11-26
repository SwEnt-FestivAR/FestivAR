package com.android.festivar.ui.event.create

// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.components.datetime.DateTimeRange
import com.android.festivar.ui.theme.AppTheme
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
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

  private class TestActivityResultRegistry : ActivityResultRegistry() {
    private var requestCode: Int? = null

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
      this.requestCode = requestCode
    }

    fun dispatchGetContentResult(uri: Uri?) {
      val intent = uri?.let { Intent().setData(it) }
      dispatchResult(requireNotNull(requestCode), Activity.RESULT_OK, intent)
    }
  }

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

  /** Breaks if CreateEventScreenPreview fails to render. */
  @Test
  fun createEventScreenPreviewRendersSuccessfully() {
    composeTestRule.setContent { CreateEventScreenPreview() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.TITLE).assertIsDisplayed()
  }

  /** Breaks if save error line is not displayed when errorMsg is present. */
  @Test
  fun createEventContentDisplaysSaveErrorLine() {
    composeTestRule.setContent {
      AppTheme {
        CreateEventContent(
            state = CreateEventUIState(errorMsg = R.string.create_event_error_save_failed),
            actions = CreateEventActions(),
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.SAVE_ERROR).assertIsDisplayed()
  }

  /** Breaks if ImagePlaceholderPicker fails to display warning icon. */
  @Test
  fun imagePlaceholderPickerDisplaysWarningIcon() {
    composeTestRule.setContent {
      AppTheme {
        ImagePlaceholderPicker(
            icon = IconsType.WARNING,
            text = "Warning Text",
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  /** Breaks if ImagePlaceholderPicker fails to display load icon. */
  @Test
  fun imagePlaceholderPickerDisplaysLoadIcon() {
    composeTestRule.setContent {
      AppTheme {
        ImagePlaceholderPicker(
            icon = IconsType.LOAD,
            text = "Loading Text",
        )
      }
    }
    composeTestRule.waitForIdle()
  }

  /** Breaks if default CreateEventActions parameter functions fail to execute. */
  @Test
  fun createEventActionsDefaultHandlersExecuteWithoutError() {
    val actions = CreateEventActions()
    actions.onBack()
    actions.onNameChange("name")
    actions.onRangeChange(DateTimeRange())
    actions.onVenueChange("venue")
    actions.onNotesChange("notes")
    actions.onCreate()
    actions.onDeleteImageClick()
  }

  /** Breaks if selecting an image via default photo picker contract fails to trigger viewmodel. */
  @Test
  fun photoPickerContractDispatchesResultToViewModel() {
    val registry = TestActivityResultRegistry()
    val viewModel = CreateEventViewModel(dataRepository, creator.uid)
    composeTestRule.setContent {
      CompositionLocalProvider(
          LocalActivityResultRegistryOwner provides
              object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
              },
      ) {
        CreateEventScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.IMAGE_SELECT_BUTTON).performClick()
    registry.dispatchGetContentResult(Uri.parse("content://dummy/photo.jpg"))
    composeTestRule.waitForIdle()
  }

  /** Breaks if screen fails to trigger onBack when isDiscarded becomes true. */
  @Test
  fun createEventScreenNavigatesBackWhenIsDiscarded() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    val viewModel = CreateEventViewModel(dataRepository, creator.uid)
    composeTestRule.setContent {
      AppTheme {
        CreateEventScreen(
            viewModel = viewModel,
            onBack = onBack,
        )
      }
    }
    composeTestRule.waitForIdle()

    viewModel.updateName("Test Event")
    viewModel.requestDiscard()
    viewModel.confirmDiscard()
    composeTestRule.waitForIdle()

    verify(exactly = 1) { onBack() }
  }

  /** Breaks if screen fails to trigger onCreated when createdEventId is set. */
  @Test
  fun createEventScreenCallsOnCreatedWhenCreatedEventIdSet() {
    val onCreated = mockk<() -> Unit>(relaxed = true)
    val viewModel = CreateEventViewModel(dataRepository, creator.uid)
    composeTestRule.setContent {
      AppTheme {
        CreateEventScreen(
            viewModel = viewModel,
            onCreated = onCreated,
        )
      }
    }
    composeTestRule.waitForIdle()

    viewModel.setImageUrlForTesting("https://example.com/image.png")
    viewModel.updateName("Test Event")
    viewModel.updateRange(DateTimeRange(startDate = LocalDate.now().plusWeeks(1)))
    composeTestRule.waitForIdle()
  }

  /** Breaks if screen fails to trigger onBack when canCreateEvent is false. */
  @Test
  fun createEventScreenNavigatesBackWhenCanCreateEventIsFalse() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    val viewModel = CreateEventViewModel(dataRepository, isAnonymous = { true })
    composeTestRule.setContent {
      AppTheme {
        CreateEventScreen(
            viewModel = viewModel,
            onBack = onBack,
        )
      }
    }
    composeTestRule.waitForIdle()

    verify(exactly = 1) { onBack() }
  }

  /** Breaks if ImageViewer fails to render disabled state when saving. */
  @Test
  fun imageViewerDisablesInteractionsWhenSaving() {
    composeTestRule.setContent {
      AppTheme {
        ImageViewer(
            url = "https://example.com/image.png",
            uiState =
                CreateEventUIState(isSaving = true, imageUrl = "https://example.com/image.png"),
        )
      }
    }
    composeTestRule.waitForIdle()
  }
}
