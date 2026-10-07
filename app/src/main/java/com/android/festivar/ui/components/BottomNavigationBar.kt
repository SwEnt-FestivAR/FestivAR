// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.festivar.R

object BottomNavigationBarTestTags {
  const val BOTTOM_NAVIGATION_BAR = "bottomNavigationBar"

  fun getTestTagForTab(tab: NavigationTab): String = "navigationTab${tab.name}"
}

/**
 * Top-level destinations of the app, shown as the items of the [BottomNavigationBar].
 *
 * @property label The text shown under the icon.
 * @property icon The drawable of the icon.
 */
enum class NavigationTab(val label: String, @DrawableRes val icon: Int) {
  EVENTS("Events", R.drawable.ic_event),
  MY_TASKS("My tasks", R.drawable.ic_pennant),
  MAP("Map", R.drawable.ic_map),
  ME("Me", R.drawable.ic_person),
}

/**
 * Navigation bar of the design, at the bottom of the top-level screens: the [selectedTab] has an
 * ink icon on a lime pill, the other ones are muted.
 *
 * @param selectedTab The tab of the screen currently shown.
 * @param onTabSelected Called when the user clicks on a tab.
 */
@Composable
fun BottomNavigationBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.testTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)) {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
      NavigationTab.entries.forEach { tab ->
        val selected = tab == selectedTab
        NavigationBarItem(
            selected = selected,
            onClick = { onTabSelected(tab) },
            icon = { Icon(painter = painterResource(tab.icon), contentDescription = null) },
            label = {
              Text(
                  text = tab.label,
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
