// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarPreviewsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun avatarPreview_showsAllAvatarFallbacks() {
    composeTestRule.setContent { AvatarPreview() }

    composeTestRule.onNodeWithText("AK").assertIsDisplayed()
    composeTestRule.onNodeWithText("SA").assertIsDisplayed()
    composeTestRule
        .onNodeWithContentDescription("User avatar", useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun profilePicturePreview_showsProfilePicture() {
    composeTestRule.setContent { AvatarProfilePicturePreview() }

    composeTestRule.onAllNodesWithTag(AvatarTestTags.PROFILE_PICTURE).assertCountEquals(1)
  }
}
