// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

object BottomNavigationBarTestTags {
  const val BOTTOM_NAVIGATION_BAR = "bottomNavigationBar"

  fun getTestTagForTab(tab: NavigationTab): String = "navigationTab${tab.name}"

  fun getTestTagForSubScreen(subScreen: NavigationSubScreen): String =
      "navigationSubScreen${subScreen.name}"
}

/**
 * Top-level destinations shown as the items of the [BottomNavigationBar].
 *
 * @property label The string resource of the text shown under the icon.
 * @property icon The drawable of the icon.
 */
enum class NavigationTab(@StringRes val label: Int, @DrawableRes val icon: Int) {
  EVENTS(R.string.nav_events, R.drawable.ic_event),
  MY_TASKS(R.string.nav_my_tasks, R.drawable.ic_pennant),
  PROFILE(R.string.nav_profile, R.drawable.ic_person),
}

/**
 * Screens reached from a tab, shown nested in the [BottomNavigationBar]: the tab icon greys out and
 * a lime disc with the sub-screen icon sits on top of it. Profile has no sub-screen.
 *
 * @property label The string resource of the screen name, read by accessibility services after the
 *   tab name.
 * @property icon The drawable of the icon on the lime disc.
 */
enum class NavigationSubScreen(@StringRes val label: Int, @DrawableRes val icon: Int) {
  /** The details of an event, under Events. */
  EVENT_DETAILS(R.string.nav_event_details, R.drawable.ic_info),
  /** The task board of an event, under Events. */
  EVENT_TASKS(R.string.nav_event_tasks, R.drawable.ic_task_alt),
  /** The details of a task, under Events or under My tasks. */
  TASK_DETAILS(R.string.nav_task_details, R.drawable.ic_task_details),
}

private val BarShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/**
 * Bottom bar of the design, on the screens of the three tabs: the [selectedTab] has an ink icon on
 * a lime pill and a bold label, the other ones are muted. On a sub-screen ([subScreen] set) the
 * selected tab keeps its bold label, its icon greys out and slides left, and a lime disc with the
 * sub-screen icon sits on top. Tapping a tab always reports it, so the caller decides whether to
 * open the tab or to go back to its start.
 *
 * @param selectedTab The tab of the screen currently shown.
 * @param onTabSelected Called with the tab the user tapped.
 * @param subScreen The sub-screen of [selectedTab] currently shown, or null on the start screen of
 *   the tab. Profile has no sub-screen.
 */
@Composable
fun BottomNavigationBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
    subScreen: NavigationSubScreen? = null,
) {
  val edge = MaterialTheme.colorScheme.outlineVariant
  val barShadow = MaterialTheme.colorScheme.onSurface.copy(alpha = BarShadowAlpha)
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .shadow(
                  elevation = 6.dp,
                  shape = BarShape,
                  ambientColor = barShadow,
                  spotColor = barShadow,
              )
              .background(MaterialTheme.colorScheme.surface, BarShape)
              .drawBehind { drawTopEdge(edge) }
              .height(80.dp)
              .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp)
              .testTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
  ) {
    NavigationTab.entries.forEach { tab ->
      val selected = tab == selectedTab
      BarItem(
          tab = tab,
          selected = selected,
          subScreen = subScreen.takeIf { selected },
          onClick = { onTabSelected(tab) },
          modifier = Modifier.weight(1f).fillMaxHeight(),
      )
    }
  }
}

/** One tab: the icon slot over the label, the whole column being the tap target. */
@Composable
private fun BarItem(
    tab: NavigationTab,
    selected: Boolean,
    subScreen: NavigationSubScreen?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val tabLabel = stringResource(tab.label)
  val description = subScreen?.let { "$tabLabel, ${stringResource(it.label)}" } ?: tabLabel
  Column(
      modifier =
          modifier
              .selectable(selected = selected, role = Role.Tab, onClick = onClick)
              .semantics { contentDescription = description }
              .testTag(BottomNavigationBarTestTags.getTestTagForTab(tab)),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Box(
        modifier = Modifier.size(width = 96.dp, height = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
      if (subScreen != null) NestedIcons(tab = tab, subScreen = subScreen)
      else RootIcon(tab = tab, selected = selected)
    }
    Text(
        text = tabLabel,
        style =
            MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                letterSpacing = if (selected) 0.em else 0.01.em,
            ),
        color =
            if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** The tab icon, on a lime pill when the tab is selected. */
@Composable
private fun RootIcon(tab: NavigationTab, selected: Boolean) {
  Box(
      modifier =
          Modifier.size(width = 64.dp, height = 32.dp)
              .background(
                  if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                  CircleShape,
              ),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        painter = painterResource(tab.icon),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** The greyed tab icon with the lime sub-screen disc overlapping its right side. */
@Composable
private fun NestedIcons(tab: NavigationTab, subScreen: NavigationSubScreen) {
  val discShadow = MaterialTheme.colorScheme.scrim.copy(alpha = DiscShadowAlpha)
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
        modifier =
            Modifier.size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .padding(start = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
      Icon(
          painter = painterResource(tab.icon),
          contentDescription = null,
          modifier = Modifier.size(20.dp).alpha(0.6f),
          tint = MaterialTheme.colorScheme.onSurface,
      )
    }
    Box(
        modifier =
            Modifier.offset(x = (-6).dp)
                .size(40.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = discShadow,
                    spotColor = discShadow,
                )
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .testTag(BottomNavigationBarTestTags.getTestTagForSubScreen(subScreen)),
        contentAlignment = Alignment.Center,
    ) {
      Icon(
          painter = painterResource(subScreen.icon),
          contentDescription = null,
          modifier = Modifier.size(22.dp),
          tint = MaterialTheme.colorScheme.onPrimary,
      )
    }
  }
}

/** Shadow strength of the design: the theme's onSurface at 6 percent under the bar. */
private const val BarShadowAlpha = 0.06f

/** Shadow strength of the design: the theme's scrim at 12 percent under the disc. */
private const val DiscShadowAlpha = 0.12f

/** The hairline along the top of the bar, following its rounded corners and nothing else. */
private fun DrawScope.drawTopEdge(color: Color) {
  val radius = 20.dp.toPx()
  val stroke = 1.dp.toPx()
  val path =
      Path().apply {
        moveTo(0f, radius)
        arcTo(
            rect =
                Rect(
                    Offset(0f, 0f),
                    Size(radius * 2, radius * 2),
                ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
        lineTo(size.width - radius, 0f)
        arcTo(
            rect =
                Rect(
                    Offset(size.width - radius * 2, 0f),
                    Size(radius * 2, radius * 2),
                ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false,
        )
      }
  drawPath(path, color, style = Stroke(width = stroke))
}

@Preview(showBackground = true)
@Composable
internal fun BottomNavigationBarPreview() {
  AppTheme { BottomNavigationBar(selectedTab = NavigationTab.EVENTS, onTabSelected = {}) }
}

@Preview(showBackground = true)
@Composable
internal fun BottomNavigationBarEventTasksPreview() {
  AppTheme {
    BottomNavigationBar(
        selectedTab = NavigationTab.EVENTS,
        onTabSelected = {},
        subScreen = NavigationSubScreen.EVENT_TASKS,
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun BottomNavigationBarMyTaskDetailsPreview() {
  AppTheme {
    BottomNavigationBar(
        selectedTab = NavigationTab.MY_TASKS,
        onTabSelected = {},
        subScreen = NavigationSubScreen.TASK_DETAILS,
    )
  }
}
