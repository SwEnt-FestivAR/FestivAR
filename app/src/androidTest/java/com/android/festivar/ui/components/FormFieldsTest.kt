// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
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
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
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

  // The field displays the value it is given, and its label is visible.
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

  // Typing calls onValueChange with the new text, and the field shows it once the state is updated.
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

  // A PasswordVisualTransformation makes the field a password field, so the text is masked and
  // not exposed to accessibility services.
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

  // Counterpart of the test above: a regular field must not be treated as a password field.
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

  // The trailing icon slot (e.g. the show/hide password button) stays clickable.
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

  // Typing past maxLength must report only the first maxLength characters, so a form never holds
  // more than its limit; the field then shows that cut-off text.
  @Test
  fun textField_cutsTypedTextAtMaxLength() {
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
            maxLength = 5,
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule.onNodeWithTag(fieldTag).performTextInput("abcdefgh")

    assertEquals("abcde", typed)
    composeTestRule.onNodeWithTag(fieldTag).assertTextContains("abcde")
  }

  // Typing in the middle of a full field must be refused: breaks if the letter goes in and the end
  // of the text is cut off instead.
  @Test
  fun textField_whenFull_refusesTextAddedInTheMiddle() {
    var typed = "abcde"
    composeTestRule.setContent {
      AppTheme {
        var value by remember { mutableStateOf(typed) }
        FestivarTextField(
            value = value,
            onValueChange = {
              value = it
              typed = it
            },
            label = "Title",
            maxLength = 5,
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule.onNodeWithTag(fieldTag).performTextInputSelection(TextRange(2))
    composeTestRule.onNodeWithTag(fieldTag).performTextInput("X")

    assertEquals("abcde", typed)
    composeTestRule.onNodeWithTag(fieldTag).assertTextContains("abcde")
  }

  // Without a maxLength, a field must still stop at DEFAULT_MAX_LENGTH: breaks if the default limit
  // is dropped or changed.
  @Test
  fun textField_byDefault_stopsAtTheDefaultMaxLength() {
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

    composeTestRule.onNodeWithTag(fieldTag).performTextInput("a".repeat(DEFAULT_MAX_LENGTH + 5))

    assertEquals(DEFAULT_MAX_LENGTH, typed.length)
  }

  // The counter must show "length/maxLength" and follow what is typed. It is a child of the field,
  // which merges it, so it is looked up in the unmerged tree.
  @Test
  fun textField_withACounter_showsTheLengthAndUpdatesWhileTyping() {
    composeTestRule.setContent {
      AppTheme {
        var value by remember { mutableStateOf("abc") }
        FestivarTextField(
            value = value,
            onValueChange = { value = it },
            label = "Description",
            showCounter = true,
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }
    val counter =
        composeTestRule.onNodeWithTag(FestivarTextFieldTestTags.COUNTER, useUnmergedTree = true)

    counter.assertIsDisplayed().assertTextEquals("3/$DEFAULT_MAX_LENGTH")

    composeTestRule.onNodeWithTag(fieldTag).performTextInput("de")

    counter.assertTextEquals("5/$DEFAULT_MAX_LENGTH")
  }

  // Counterpart of the test above: a field with the default showCounter = false has no counter.
  @Test
  fun textField_byDefault_hasNoCounter() {
    composeTestRule.setContent {
      AppTheme {
        FestivarTextField(
            value = "abc",
            onValueChange = {},
            label = "Title",
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule
        .onNodeWithTag(FestivarTextFieldTestTags.COUNTER, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  // The picker field displays the value it is given, e.g. the formatted date.
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

  // Tapping the read-only picker field calls onClick exactly once, which is what opens the dialog.
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

  // The error state of the picker field must reach accessibility services, not only the colors:
  // Material marks an invalid field with the Error semantics.
  @Test
  fun pickerField_withAnError_isMarkedAsInvalid() {
    composeTestRule.setContent {
      AppTheme {
        FestivarPickerField(
            value = "",
            label = "Start date",
            onClick = {},
            isError = true,
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule
        .onNodeWithTag(fieldTag)
        .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
  }

  // Counterpart of the test above: a field with the default isError = false must not be flagged.
  @Test
  fun pickerField_withoutAnError_isNotMarkedAsInvalid() {
    composeTestRule.setContent {
      AppTheme {
        FestivarPickerField(
            value = "",
            label = "Start date",
            onClick = {},
            modifier = Modifier.testTag(fieldTag),
        )
      }
    }

    composeTestRule
        .onNodeWithTag(fieldTag)
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
  }

  // The error line displays exactly the message it is given.
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
