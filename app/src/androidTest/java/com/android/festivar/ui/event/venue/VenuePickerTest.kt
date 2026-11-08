// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.venue

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Venue
import com.android.festivar.model.geocoding.GeocodingRepositoryLocal
import com.android.festivar.model.geocoding.GeocodingResult
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VenuePickerTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val rolex = GeocodingResult("Rolex Learning Center", 46.5185, 6.5684)
  private val satellite = GeocodingResult("Satellite", 46.5205, 6.5650)

  private fun setField(address: String, venue: Venue?, onVenueChange: (Venue?) -> Unit = {}) {
    composeTestRule.setContent {
      AppTheme {
        VenueField(
            address = address,
            venue = venue,
            onVenueChange = onVenueChange,
            geocoding = GeocodingRepositoryLocal(mapOf("EPFL" to listOf(rolex, satellite))),
        )
      }
    }
  }

  private fun setSheet(
      state: VenuePickerUiState,
      onCandidate: (GeocodingResult) -> Unit = {},
      onBearing: (Int) -> Unit = {},
      onConfirm: (Double, Double) -> Unit = { _, _ -> },
      onDismiss: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        VenuePickerSheet(
            address = "EPFL, Lausanne",
            state = state,
            onCandidate = onCandidate,
            onBearing = onBearing,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            mapContent = {},
        )
      }
    }
  }

  /** Catches the picker opening on a blank address, which has nothing to look up. */
  @Test
  fun blankAddressShowsNotPlacedAndDisablesPlace() {
    setField(address = " ", venue = null)
    composeTestRule.onNodeWithText("Not placed yet").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.PLACE_BUTTON).assertIsNotEnabled()
  }

  /** Catches Place staying disabled once an address is typed. */
  @Test
  fun typedAddressEnablesPlace() {
    setField(address = "EPFL", venue = null)
    composeTestRule.onNodeWithTag(VenuePickerTestTags.PLACE_BUTTON).assertIsEnabled()
  }

  /** Catches a placed venue not summarised, or the summary changing with the device locale. */
  @Test
  fun placedVenueShowsSummaryChangeAndRemove() {
    setField(address = "EPFL", venue = Venue(46.517, 6.599, 12.0))
    composeTestRule
        .onNodeWithTag(VenuePickerTestTags.SUMMARY)
        .assertTextEquals("46.51700, 6.59900 · north at 12°")
    composeTestRule.onNodeWithTag(VenuePickerTestTags.CHANGE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.REMOVE_BUTTON).assertIsDisplayed()
  }

  /** Catches Remove not clearing the venue. */
  @Test
  fun removeClearsTheVenue() {
    var changed: Venue? = Venue(46.517, 6.599, 12.0)
    var calls = 0
    setField(address = "EPFL", venue = changed) {
      changed = it
      calls++
    }
    composeTestRule.onNodeWithTag(VenuePickerTestTags.REMOVE_BUTTON).performClick()
    assertEquals(1, calls)
    assertEquals(null, changed)
  }

  /** Catches no feedback while the address is looked up. */
  @Test
  fun sheetShowsSearching() {
    setSheet(VenuePickerUiState(isSearching = true))
    composeTestRule.onNodeWithTag(VenuePickerTestTags.SEARCHING).assertIsDisplayed()
  }

  /** Catches candidates not listed or a tap sending the wrong one. */
  @Test
  fun tappingACandidateSendsIt() {
    var tapped: GeocodingResult? = null
    setSheet(
        VenuePickerUiState(candidates = listOf(rolex, satellite)),
        onCandidate = { tapped = it },
    )
    composeTestRule.onNodeWithTag(VenuePickerTestTags.candidate(0)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.candidate(1)).performClick()
    assertEquals(satellite, tapped)
  }

  /** Catches an empty search leaving the organizer without a hint. */
  @Test
  fun sheetShowsNoResults() {
    setSheet(VenuePickerUiState(searched = true))
    composeTestRule.onNodeWithTag(VenuePickerTestTags.NO_RESULTS).assertIsDisplayed()
  }

  /** Catches the no match line flashing before the first lookup has run. */
  @Test
  fun sheetShowsNoHintBeforeTheFirstLookup() {
    setSheet(VenuePickerUiState())
    composeTestRule.onNodeWithTag(VenuePickerTestTags.NO_RESULTS).assertDoesNotExist()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.SEARCHING).assertDoesNotExist()
  }

  /** Catches a failed lookup shown as no match or not at all. */
  @Test
  fun sheetShowsSearchError() {
    setSheet(VenuePickerUiState(searchFailed = true))
    composeTestRule.onNodeWithTag(VenuePickerTestTags.SEARCH_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.NO_RESULTS).assertDoesNotExist()
  }

  /** Catches the slider not reporting the bearing in whole degrees. */
  @Test
  fun movingTheSliderSendsTheBearing() {
    var bearing: Int? = null
    setSheet(VenuePickerUiState(), onBearing = { bearing = it })
    composeTestRule.onNodeWithText("Plan rotation: 0° from north").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.BEARING_SLIDER).performSemanticsAction(
        SemanticsActions.SetProgress
    ) {
      it(90f)
    }
    assertEquals(90, bearing)
  }

  /** Catches Confirm not reading the camera or firing twice. */
  @Test
  fun confirmSendsTheCameraTargetOnce() {
    val confirmed = mutableListOf<Pair<Double, Double>>()
    setSheet(VenuePickerUiState(), onConfirm = { lat, lng -> confirmed += lat to lng })
    composeTestRule.onNodeWithTag(VenuePickerTestTags.CONFIRM_BUTTON).performClick()
    assertEquals(listOf(0.0 to 0.0), confirmed)
  }

  /** Catches Cancel not closing the sheet. */
  @Test
  fun cancelDismisses() {
    var dismissed = 0
    setSheet(VenuePickerUiState(), onDismiss = { dismissed++ })
    composeTestRule.onNodeWithTag(VenuePickerTestTags.CANCEL_BUTTON).performClick()
    assertEquals(1, dismissed)
  }

  /** Catches the real map crashing the sheet without tiles or hiding the pin. */
  @Test
  fun realMapShowsTheCenterPin() {
    composeTestRule.setContent {
      AppTheme {
        VenuePickerSheet(
            address = "EPFL, Lausanne",
            state = VenuePickerUiState(center = null),
            onCandidate = {},
            onBearing = {},
            onConfirm = { _, _ -> },
            onDismiss = {},
        )
      }
    }
    composeTestRule.onNodeWithTag(VenuePickerTestTags.CENTER_PIN).assertExists()
  }

  /** Catches the field not wiring the sheet: Place opens it, looks up, and Confirm saves. */
  @Test
  fun placeOpensTheSheetAndConfirmSavesAVenue() {
    var changed: Venue? = null
    setField(address = "EPFL", venue = null) { changed = it }
    composeTestRule.onNodeWithTag(VenuePickerTestTags.PLACE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.SHEET).assertExists()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.candidate(1)).assertExists()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.CONFIRM_BUTTON).performClick()
    assertNotNull(changed)
    composeTestRule.onNodeWithTag(VenuePickerTestTags.SHEET).assertDoesNotExist()
  }

  /** Catches a preview that no longer composes. */
  @Test
  fun previewsCompose() {
    composeTestRule.setContent {
      VenueFieldNotPlacedPreview()
      VenueFieldPlacedPreview()
    }
    composeTestRule.onNodeWithTag(VenuePickerTestTags.PLACE_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(VenuePickerTestTags.SUMMARY).assertExists()
  }
}
