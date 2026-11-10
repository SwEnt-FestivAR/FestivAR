// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsFragmentTest {
  @Test
  fun themePreferenceAppliesSelectedNightMode() {
    assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeForTheme("light"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeForTheme("dark"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("system"))
  }

  @Test
  fun settingsFragmentMapsThemeAndLanguagePreferences() {
    assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeForTheme("light"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeForTheme("dark"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("system"))
    assertEquals(LocaleListCompat.forLanguageTags("fr"), localeFromString("fr"))
    assertEquals(LocaleListCompat.forLanguageTags("en"), localeFromString("en"))
    assertEquals(LocaleListCompat.getDefault(), localeFromString("system"))
  }
}
