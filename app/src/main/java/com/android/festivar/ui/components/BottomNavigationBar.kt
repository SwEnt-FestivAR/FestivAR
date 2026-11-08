// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

object BottomNavigationBarTestTags {
  const val BOTTOM_NAVIGATION_BAR = "bottomNavigationBar"
  const val TASKS_BADGE = "navigationTasksBadge"

  fun getTestTagForTab(tab: NavigationTab): String = "navigationTab${tab.name}"
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
 * Navigation bar of the design, at the bottom of the top-level screens: the [selectedTab] has an
 * ink icon on a lime pill, the other ones are muted.
 *
 * @param selectedTab The tab of the screen currently shown.
 * @param onTabSelected Called when the user clicks on a tab.
 * @param inEventTasks Only meaningful when [selectedTab] is [NavigationTab.EVENTS]: the user is
 *   inside the tasks of an event, so no tab is highlighted and the Events icon carries a tasks
 *   badge.
 */
@Composable
fun BottomNavigationBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
    inEventTasks: Boolean = false,
) {
  Column(modifier = modifier.testTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)) {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
      NavigationTab.entries.forEach { tab ->
        val selected = tab == selectedTab && !inEventTasks
        val showBadge = inEventTasks && tab == NavigationTab.EVENTS
        NavigationBarItem(
            selected = selected,
            onClick = { onTabSelected(tab) },
            icon = {
              Box {
                Icon(painter = painterResource(tab.icon), contentDescription = null)
                if (showBadge) TasksBadge(modifier = Modifier.align(Alignment.TopStart))
              }
            },
            label = {
              Text(
                  text = stringResource(tab.label),
                  style =
                      MaterialTheme.typography.labelMedium.copy(
                          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                      ),
              )
            },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier = Modifier.testTag(BottomNavigationBarTestTags.getTestTagForTab(tab)),
        )
      }
    }
  }
}

/** Pennant on a lime disc, over the top right of the Events icon. */
@Composable
private fun TasksBadge(modifier: Modifier = Modifier) {
  Box(
      modifier =
          modifier
              .offset(x = 16.dp, y = (-2).dp)
              .size(22.dp)
              .background(MaterialTheme.colorScheme.primary, CircleShape)
              .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
              .testTag(BottomNavigationBarTestTags.TASKS_BADGE),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        painter = painterResource(R.drawable.ic_pennant),
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.onPrimary,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavigationBarPreview() {
  AppTheme { BottomNavigationBar(selectedTab = NavigationTab.EVENTS, onTabSelected = {}) }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavigationBarEventTasksPreview() {
  AppTheme {
    BottomNavigationBar(
        selectedTab = NavigationTab.EVENTS,
        onTabSelected = {},
        inEventTasks = true,
    )
  }
}
