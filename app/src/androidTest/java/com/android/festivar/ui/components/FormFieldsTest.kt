// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormFieldsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val fieldTag = "field"

  @Test
  fun textField_showsItsLabelAndValue() {
    composeTestRule.setContent {
      AppTheme {
        FestivarTextField(
            value = "Run power to stage",
            onValueChange = {},
            label = "Title",
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule.onNodeWithTag(fieldTag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(fieldTag).assertTextContains("Run power to stage")
    composeTestRule.onNodeWithText("Title").assertIsDisplayed()
  }

  @Test
  fun textField_reportsTypedTextAndShowsIt() {
    var typed = ""
    composeTestRule.setContent {
      AppTheme {
        var value by remember { mutableStateOf("") }
        FestivarTextField(
            value = value,
            onValueChange = {
              value = it
              typed = it
            },
            label = "Title",
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule.onNodeWithTag(fieldTag).performTextInput("abc")

    assertEquals("abc", typed)
    composeTestRule.onNodeWithTag(fieldTag).assertTextContains("abc")
  }

  @Test
  fun textField_withAPasswordTransformation_isMarkedAsAPasswordField() {
    composeTestRule.setContent {
      AppTheme {
        FestivarTextField(
            value = "secret",
            onValueChange = {},
            label = "Password",
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule
        .onNodeWithTag(fieldTag)
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
  }

  @Test
  fun textField_withoutATransformation_isNotAPasswordField() {
    composeTestRule.setContent {
      AppTheme {
        FestivarTextField(
            value = "visible",
            onValueChange = {},
            label = "Title",
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule
        .onNodeWithTag(fieldTag)
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
  }

  @Test
  fun textField_trailingIconIsInteractive() {
    var clicks = 0
    composeTestRule.setContent {
      AppTheme {
        FestivarTextField(
            value = "",
            onValueChange = {},
            label = "Password",
            trailingIcon = {
              IconButton(onClick = { clicks++ }, modifier = Modifier.testTag("trailing")) {
                Text("show")
              }
            },
        )
      }
    }

    composeTestRule.onNodeWithTag("trailing").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, clicks)
  }

  @Test
  fun pickerField_showsItsValue() {
    composeTestRule.setContent {
      AppTheme {
        FestivarPickerField(
            value = "Sat 17 Oct",
            label = "Start date",
            onClick = {},
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule.onNodeWithTag(fieldTag).assertTextContains("Sat 17 Oct")
  }

  @Test
  fun pickerField_callsOnClickWhenTapped() {
    var clicks = 0
    composeTestRule.setContent {
      AppTheme {
        FestivarPickerField(
            value = "",
            label = "Start date",
            onClick = { clicks++ },
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, clicks)
  }

  @Test
  fun fieldError_showsItsMessage() {
    composeTestRule.setContent {
      AppTheme {
        FestivarFieldError(
            message = "Title cannot be empty",
            modifier = Modifier.testTag("error"),
        )
      }
    }

    composeTestRule.onNodeWithTag("error").assertIsDisplayed()
    composeTestRule.onNodeWithTag("error").assertTextEquals("Title cannot be empty")
  }
}
