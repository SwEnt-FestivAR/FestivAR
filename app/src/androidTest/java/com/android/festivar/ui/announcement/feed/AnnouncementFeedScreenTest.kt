// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.announcement.AnnouncementPriority
import com.android.festivar.ui.components.EmptyStateTestTags
import com.android.festivar.ui.components.RefreshableListTestTags
import com.android.festivar.ui.theme.AppTheme
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val EVENT_ID = "e1"
private const val SCREEN = "screen"

class AnnouncementFeedScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val zone = ZoneId.of("Europe/Zurich")

  /** The day of the Figma, a Saturday. */
  private val today = LocalDate.of(2026, 10, 17)
  private val yesterday = today.minusDays(1)

  private fun at(date: LocalDate, hour: Int, minute: Int = 0): ZonedDateTime =
      date.atTime(hour, minute).atZone(zone)

  // The six announcements of the Figma frame 3329:8730. The Figma cuts off the text of the last
  // one, an info of yesterday morning, so its texts are made up.
  private val pinned =
      Announcement(
          announcementId = "pinned",
          title = "Gates and opening hours",
          authorId = "u0",
          content =
              "Main gate opens at 10:00, north gate at 12:00. Last entry is 23:00 on both days.",
          updatedAt = at(yesterday, 9),
          pinned = true,
      )
  private val urgent =
      Announcement(
          announcementId = "urgent",
          title = "Stage B is closed",
          authorId = "u0",
          content = "Strong wind on the hill. Send everyone to the north gate until 16:00.",
          priority = AnnouncementPriority.URGENT,
          updatedAt = at(today, 14, 2),
      )
  private val warning =
      Announcement(
          announcementId = "warning",
          title = "Headliner moved to 21:30",
          authorId = "u0",
          content = "The band's van is late. Doors stay at 19:00, the main set starts later.",
          priority = AnnouncementPriority.WARNING,
          updatedAt = at(today, 11, 30),
      )
  private val water =
      Announcement(
          announcementId = "water",
          title = "Free water at the info tent",
          authorId = "u0",
          content = "Refill points are open by the info tent and next to the food trucks all day.",
          updatedAt = at(today, 9, 15),
      )
  private val lostAndFound =
      Announcement(
          announcementId = "lost",
          title = "Lost and found",
          authorId = "u0",
          content =
              "A blue backpack was left at the entrance. Come to the info tent if it's yours.",
          updatedAt = at(yesterday, 16, 5),
      )
  private val shuttle =
      Announcement(
          announcementId = "shuttle",
          title = "Night shuttle to the station",
          authorId = "u0",
          content = "Shuttles leave the north gate every 20 minutes after midnight.",
          updatedAt = at(yesterday, 10),
      )

  /** The feed of the Figma, as the ViewModel sorts it. */
  private val figmaState =
      AnnouncementFeedUIState(
          pinnedAnnouncements = listOf(pinned),
          sections =
              listOf(
                  AnnouncementFeedSection(today, listOf(urgent, warning, water)),
                  AnnouncementFeedSection(yesterday, listOf(lostAndFound, shuttle)),
              ),
      )

  /** What the content reports back through its callbacks. */
  private class Callbacks {
    val filters = mutableListOf<AnnouncementFilter>()
    val selected = mutableListOf<Announcement>()
    var refreshes = 0
    var backs = 0
    var adds = 0
  }

  /**
   * Shows [AnnouncementFeedContent] with [uiState], relative to [today]. A [tall] content lays out
   * the whole Figma feed at once, to compare the positions of all its rows.
   */
  private fun setContent(
      uiState: AnnouncementFeedUIState,
      canAddAnnouncement: Boolean = true,
      tall: Boolean = false,
  ): Callbacks {
    val callbacks = Callbacks()
    composeTestRule.setContent {
      AppTheme {
        Box(if (tall) Modifier.requiredHeight(1400.dp) else Modifier) {
          AnnouncementFeedContent(
              uiState = uiState,
              canAddAnnouncement = canAddAnnouncement,
              today = today,
              onSelectFilter = { callbacks.filters += it },
              onRefresh = { callbacks.refreshes++ },
              onBack = { callbacks.backs++ },
              onAddAnnouncement = { callbacks.adds++ },
              onSelectAnnouncement = { callbacks.selected += it },
          )
        }
      }
    }
    return callbacks
  }

  /** The texts of the row of [announcement], in reading order. */
  private fun rowTexts(announcement: Announcement): List<String> =
      composeTestRule
          .onNodeWithTag(AnnouncementFeedScreenTestTags.announcementRow(announcement))
          .fetchSemanticsNode()
          .config[SemanticsProperties.Text]
          .map { it.text }

  private fun top(tag: String): Float =
      composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().top.value

  // Breaks if a chip is missing, out of order (All first, then by priority), shows a wrong count,
  // or if the selected filter is not the one marked selected.
  @Test
  fun chipsShowEachFilterWithItsCountAllFirst() {
    setContent(figmaState)

    val expected =
        listOf(
            AnnouncementFilter.ALL to ("All" to "6"),
            AnnouncementFilter.URGENT to ("Urgent" to "1"),
            AnnouncementFilter.WARNING to ("Warning" to "1"),
            AnnouncementFilter.INFO to ("Info" to "4"),
        )
    val lefts = expected.map { (filter, labelAndCount) ->
      val (label, count) = labelAndCount
      val tag = AnnouncementFeedScreenTestTags.filterChip(filter)
      composeTestRule
          .onNode(hasTestTag(tag).and(hasText(label)).and(hasText(count)))
          .assertIsDisplayed()
      composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().left.value
    }
    assertEquals(lefts.sorted(), lefts)
    // ALL is selected by default
    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.filterChip(AnnouncementFilter.ALL))
        .assertIsSelected()
    expected.drop(1).forEach { (filter, _) ->
      composeTestRule
          .onNodeWithTag(AnnouncementFeedScreenTestTags.filterChip(filter))
          .assertIsNotSelected()
    }
  }

  // Breaks if the French chip labels get long again (e.g. "Avertissement"), so that the last chip
  // is clipped on a 360dp phone.
  @Test
  fun frenchChipsFitOnA360dpPhone() {
    composeTestRule.setContent {
      val configuration =
          Configuration(LocalConfiguration.current).apply { setLocale(Locale.FRENCH) }
      val context = LocalContext.current.createConfigurationContext(configuration)
      CompositionLocalProvider(
          LocalConfiguration provides configuration,
          LocalContext provides context,
          LocalResources provides context.resources,
      ) {
        AppTheme {
          Box(Modifier.requiredWidth(360.dp).testTag(SCREEN)) {
            AnnouncementFeedContent(
                uiState = figmaState,
                canAddAnnouncement = true,
                today = today,
            )
          }
        }
      }
    }

    val screen = composeTestRule.onNodeWithTag(SCREEN).getUnclippedBoundsInRoot()
    composeTestRule
        .onNode(
            hasTestTag(AnnouncementFeedScreenTestTags.filterChip(AnnouncementFilter.WARNING))
                .and(hasText("Alerte"))
        )
        .assertExists()
    AnnouncementFilter.entries.forEach {
      val chip =
          composeTestRule
              .onNodeWithTag(AnnouncementFeedScreenTestTags.filterChip(it))
              .getUnclippedBoundsInRoot()
      assertTrue("$it ends at ${chip.right}, past ${screen.right}", chip.right <= screen.right)
    }
  }

  // Breaks if a chip reports another filter than its own.
  @Test
  fun tappingAChipSelectsItsFilter() {
    val callbacks = setContent(figmaState)

    AnnouncementFilter.entries.forEach {
      composeTestRule.onNodeWithTag(AnnouncementFeedScreenTestTags.filterChip(it)).performClick()
    }

    assertEquals(AnnouncementFilter.entries, callbacks.filters)
  }

  // Breaks if the pinned announcements are not on top under their header, or if the days are not
  // each under their own header, newest first, with their announcements in the given order.
  @Test
  fun pinnedComeFirstThenEachDayUnderItsHeaderNewestFirst() {
    setContent(figmaState, tall = true)

    val tags = AnnouncementFeedScreenTestTags
    val order =
        listOf(
            tags.PINNED_HEADER,
            tags.announcementRow(pinned),
            tags.dateHeader(today),
            tags.announcementRow(urgent),
            tags.announcementRow(warning),
            tags.announcementRow(water),
            tags.dateHeader(yesterday),
            tags.announcementRow(lostAndFound),
            tags.announcementRow(shuttle),
        )
    val tops = order.map { top(it) }
    assertEquals(tops.sorted(), tops)
    composeTestRule.onNodeWithTag(tags.PINNED_HEADER).assertTextEquals("PINNED")
    composeTestRule.onNodeWithTag(tags.dateHeader(today)).assertTextEquals("TODAY · SAT 17 OCT")
    composeTestRule
        .onNodeWithTag(tags.dateHeader(yesterday))
        .assertTextEquals("YESTERDAY · FRI 16 OCT")
  }

  // Breaks if a day before yesterday gets a "Today" or "Yesterday" prefix instead of its date only.
  @Test
  fun olderDayHeaderShowsOnlyItsDate() {
    val older = today.minusDays(2)
    setContent(
        AnnouncementFeedUIState(
            sections =
                listOf(AnnouncementFeedSection(older, listOf(water.copy(updatedAt = at(older, 9)))))
        )
    )

    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.dateHeader(older))
        .assertTextEquals("THU 15 OCT")
  }

  // Breaks if a row loses its priority, time, title or content, if a pinned row stops showing its
  // weekday (it has no date header above it), or if a day's row shows more than its time.
  @Test
  fun rowsShowPriorityTimeTitleAndContent() {
    setContent(figmaState, tall = true)

    assertEquals(listOf("Info", "Fri 09:00", pinned.title, pinned.content), rowTexts(pinned))
    assertEquals(listOf("Urgent", "14:02", urgent.title, urgent.content), rowTexts(urgent))
    assertEquals(listOf("Warning", "11:30", warning.title, warning.content), rowTexts(warning))
  }

  // Breaks if an announcement without content still shows an empty or placeholder content line.
  @Test
  fun rowWithoutContentShowsOnlyItsHeaderAndTitle() {
    val withoutContent = water.copy(content = "  ")
    setContent(
        AnnouncementFeedUIState(
            sections = listOf(AnnouncementFeedSection(today, listOf(withoutContent)))
        )
    )

    assertEquals(listOf("Info", "09:15", water.title), rowTexts(withoutContent))
  }

  // Breaks if the selected filter is ignored: the pinned header and a day without matching
  // announcements must go away with their rows.
  @Test
  fun selectedFilterShowsOnlyItsAnnouncements() {
    setContent(figmaState.copy(selectedFilter = AnnouncementFilter.WARNING))

    val tags = AnnouncementFeedScreenTestTags
    composeTestRule.onNodeWithTag(tags.announcementRow(warning)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tags.dateHeader(today)).assertIsDisplayed()
    listOf(
            tags.PINNED_HEADER,
            tags.announcementRow(pinned),
            tags.announcementRow(urgent),
            tags.announcementRow(water),
            tags.dateHeader(yesterday),
        )
        .forEach { composeTestRule.onNodeWithTag(it).assertDoesNotExist() }
  }

  // Breaks if a tap on a row reports another announcement, or none.
  @Test
  fun tappingARowSelectsItsAnnouncement() {
    val callbacks = setContent(figmaState)

    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.announcementRow(urgent))
        .performClick()

    assertEquals(listOf(urgent), callbacks.selected)
  }

  // Breaks if the back arrow or the add button is not connected to its callback.
  @Test
  fun backAndAddCallTheirCallbacks() {
    val callbacks = setContent(figmaState)

    composeTestRule.onNodeWithTag(AnnouncementFeedScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule
        .onNode(
            hasTestTag(AnnouncementFeedScreenTestTags.ADD_ANNOUNCEMENT_FAB)
                .and(hasAnyDescendant(hasText("Add announcement"))),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.ADD_ANNOUNCEMENT_FAB)
        .performClick()

    assertEquals(1, callbacks.backs)
    assertEquals(1, callbacks.adds)
  }

  // Breaks if the add button depends on the state of the feed instead of canAddAnnouncement only.
  @Test
  fun addButtonFollowsCanAddAnnouncementInEveryState() {
    var uiState by mutableStateOf(figmaState)
    var canAdd by mutableStateOf(true)
    composeTestRule.setContent {
      AppTheme { AnnouncementFeedContent(uiState = uiState, canAddAnnouncement = canAdd) }
    }
    val states =
        listOf(
            figmaState,
            AnnouncementFeedUIState(isLoading = true),
            AnnouncementFeedUIState(),
            AnnouncementFeedUIState(errorMsg = R.string.announcement_feed_load_failed),
            figmaState.copy(errorMsg = R.string.announcement_feed_load_failed),
            AnnouncementFeedUIState(
                pinnedAnnouncements = listOf(pinned),
                selectedFilter = AnnouncementFilter.URGENT,
            ),
        )
    val fab = composeTestRule.onNodeWithTag(AnnouncementFeedScreenTestTags.ADD_ANNOUNCEMENT_FAB)

    listOf(true, false).forEach { allowed ->
      canAdd = allowed
      states.forEach {
        uiState = it
        if (allowed) fab.assertIsDisplayed() else fab.assertDoesNotExist()
      }
    }
  }

  // Breaks if the empty feed shows another empty state than its own, or offers buttons in it: only
  // the add button, which follows canAddAnnouncement, may add an announcement.
  @Test
  fun emptyFeedShowsItsEmptyStateWithoutButtons() {
    setContent(AnnouncementFeedUIState(), canAddAnnouncement = false)

    composeTestRule
        .onNodeWithTag(EmptyStateTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("No announcements yet")
    composeTestRule
        .onNodeWithText("Organizers post updates about the event here.")
        .assertIsDisplayed()
    // The back arrow and the four chips
    composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(5)
  }

  // Breaks if a filter without announcements shows the empty state of the feed instead of its own
  // message.
  @Test
  fun filterWithoutAnnouncementsShowsTheFilterMessage() {
    setContent(figmaState.copy(sections = emptyList(), selectedFilter = AnnouncementFilter.URGENT))

    composeTestRule
        .onNodeWithTag(RefreshableListTestTags.FILTER_EMPTY)
        .assertIsDisplayed()
        .assertTextEquals("Nothing here. Try another filter.")
  }

  // Breaks if the error of the state is not worded from its string resource, or if Retry does not
  // refresh the feed.
  @Test
  fun errorShowsItsMessageAndRetryRefreshes() {
    val callbacks =
        setContent(AnnouncementFeedUIState(errorMsg = R.string.announcement_feed_load_failed))

    composeTestRule
        .onNodeWithTag(RefreshableListTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals("Unable to load the announcements.")
    composeTestRule.onNodeWithTag(RefreshableListTestTags.RETRY).performClick()

    assertEquals(1, callbacks.refreshes)
  }

  /**
   * An [EventsRepositoryLocal] that counts the fetches of the event, and waits for [gate] to be
   * completed before answering while it is set.
   */
  private class CountingRepository(
      event: Event,
      @Volatile var gate: CompletableDeferred<Unit>? = null,
      private val local: EventsRepository = EventsRepositoryLocal(listOf(event)),
  ) : EventsRepository by local {
    val fetches = AtomicInteger()

    override suspend fun getEvent(eventId: String): Event {
      fetches.incrementAndGet()
      gate?.await()
      return local.getEvent(eventId)
    }
  }

  /** A [LifecycleOwner] whose state the test moves itself, e.g. to send the screen to the back. */
  private class TestLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry.createUnsafe(this)
    override val lifecycle: Lifecycle
      get() = registry
  }

  private fun figmaEvent() =
      Event(
          eventId = EVENT_ID,
          title = "Balelec",
          startDate = at(yesterday, 8),
          endDate = at(today.plusDays(1), 4),
          announcements = listOf(shuttle, water, pinned, urgent, lostAndFound, warning),
      )

  /**
   * Shows the [AnnouncementFeedScreen] of a ViewModel reading [repository], in [lifecycleOwner].
   */
  private fun setScreen(repository: EventsRepository, lifecycleOwner: LifecycleOwner? = null) {
    val viewModel =
        AnnouncementFeedViewModel(
            eventId = EVENT_ID,
            eventsRepository = repository,
            clock = Clock.fixed(at(today, 15).toInstant(), zone),
        )
    composeTestRule.setContent {
      CompositionLocalProvider(
          LocalLifecycleOwner provides (lifecycleOwner ?: LocalLifecycleOwner.current)
      ) {
        AppTheme {
          AnnouncementFeedScreen(
              viewModel = viewModel,
              canAddAnnouncement = true,
              onBack = {},
              onAddAnnouncement = {},
          )
        }
      }
    }
  }

  // Breaks if the screen does not fetch when it comes to the front, e.g. coming back from adding an
  // announcement, fetches while behind another screen, or hides the progress of the first fetch.
  @Test
  fun screenFetchesTheFeedEachTimeItComesToTheFront() {
    val gate = CompletableDeferred<Unit>()
    val repository = CountingRepository(figmaEvent(), gate = gate)
    val lifecycleOwner = TestLifecycleOwner()
    composeTestRule.runOnUiThread { lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED }
    setScreen(repository, lifecycleOwner)

    composeTestRule.onNodeWithTag(RefreshableListTestTags.LOADING).assertIsDisplayed()
    gate.complete(Unit)
    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.announcementRow(pinned))
        .assertIsDisplayed()
    assertEquals(1, repository.fetches.get())

    // Another screen comes to the front, where an urgent announcement is added
    composeTestRule.runOnUiThread { lifecycleOwner.registry.currentState = Lifecycle.State.CREATED }
    val added =
        urgent.copy(
            announcementId = "added",
            title = "Storm warning",
            updatedAt = at(today, 14, 30),
        )
    runBlocking { repository.addAnnouncement(EVENT_ID, added) }
    composeTestRule.waitForIdle()
    assertEquals(1, repository.fetches.get())

    composeTestRule.runOnUiThread { lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED }

    composeTestRule
        .onNode(
            hasTestTag(AnnouncementFeedScreenTestTags.filterChip(AnnouncementFilter.URGENT))
                .and(hasText("2"))
        )
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AnnouncementFeedScreenTestTags.announcementRow(added))
        .assertExists()
    assertEquals(2, repository.fetches.get())
  }

  // Breaks if a chip of the screen does not reach the ViewModel, so the feed is never filtered.
  @Test
  fun tappingAChipFiltersTheFeed() {
    setScreen(CountingRepository(figmaEvent()))
    val tags = AnnouncementFeedScreenTestTags
    composeTestRule.onNodeWithTag(tags.announcementRow(pinned)).assertIsDisplayed()

    composeTestRule.onNodeWithTag(tags.filterChip(AnnouncementFilter.URGENT)).performClick()

    composeTestRule.onNodeWithTag(tags.filterChip(AnnouncementFilter.URGENT)).assertIsSelected()
    composeTestRule.onNodeWithTag(tags.announcementRow(urgent)).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tags.announcementRow(pinned)).assertDoesNotExist()
    composeTestRule.onNodeWithTag(tags.announcementRow(warning)).assertDoesNotExist()
    composeTestRule.onNodeWithTag(tags.PINNED_HEADER).assertDoesNotExist()
  }
}
