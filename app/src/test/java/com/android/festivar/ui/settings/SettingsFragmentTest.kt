// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.MySettingsActivity
import com.android.festivar.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("unknown"))
  }

  @Test
  fun localeFromStringElseBranch() {
    assertEquals(LocaleListCompat.forLanguageTags("fr"), localeFromString("fr"))
    assertEquals(LocaleListCompat.forLanguageTags("en"), localeFromString("en"))
    assertEquals(LocaleListCompat.getDefault(), localeFromString("system"))
    assertEquals(LocaleListCompat.getDefault(), localeFromString("de"))
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
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            as SettingsFragment
    val preference = fragment.findPreference<DropDownPreference>("theme")

    assertTrue(preference?.callChangeListener("dark") == true)
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())

    // Test non-string value returns false
    assertFalse(preference?.callChangeListener(123) == true)
  }

  @Test
  fun changingLanguagePreferenceAppliesSelectedLocale() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment =
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            as SettingsFragment
    val preference = fragment.findPreference<DropDownPreference>("language")

    assertTrue(preference?.callChangeListener("fr") == true)
    assertEquals(LocaleListCompat.forLanguageTags("fr"), AppCompatDelegate.getApplicationLocales())

    // Test non-string value returns false
    assertFalse(preference?.callChangeListener(456) == true)
  }

  @Test
  fun settingsFragmentWithNullPreferencesInSharedPreferences() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity).edit().clear().commit()

    val fragment =
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            as SettingsFragment
    fragment.onCreatePreferences(null, null)
  }
}
