// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.MySettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
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

  @Test
  fun changingThemePreferenceAppliesSelectedNightMode() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment =
        activity.supportFragmentManager.findFragmentById(
            com.android.festivar.R.id.settings_container
        ) as SettingsFragment
    val preference = fragment.findPreference<androidx.preference.DropDownPreference>("theme")

    assertTrue(preference?.callChangeListener("dark") == true)
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
  }

  @Test
  fun changingLanguagePreferenceAppliesSelectedLocale() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment =
        activity.supportFragmentManager.findFragmentById(
            com.android.festivar.R.id.settings_container
        ) as SettingsFragment
    val preference = fragment.findPreference<androidx.preference.DropDownPreference>("language")

    assertTrue(preference?.callChangeListener("fr") == true)
    assertEquals(LocaleListCompat.forLanguageTags("fr"), AppCompatDelegate.getApplicationLocales())
  }
}
