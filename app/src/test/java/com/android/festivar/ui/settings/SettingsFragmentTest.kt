// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsFragmentTest {
  @Test
  fun themePreferenceAppliesSelectedNightMode() {
    assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeForTheme("light"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeForTheme("dark"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("system"))
  }
}
