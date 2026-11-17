// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.MySettingsActivity
import com.android.festivar.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.Locale
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
    assertEquals(LocaleListCompat.getEmptyLocaleList(), localeFromString("system"))
    assertEquals(LocaleListCompat.getEmptyLocaleList(), localeFromString("de"))
  }

  @Test
  fun settingsFragmentMapsThemeAndLanguagePreferences() {
    assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeForTheme("light"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeForTheme("dark"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("system"))
    assertEquals(LocaleListCompat.forLanguageTags("fr"), localeFromString("fr"))
    assertEquals(LocaleListCompat.forLanguageTags("en"), localeFromString("en"))
    assertEquals(LocaleListCompat.getEmptyLocaleList(), localeFromString("system"))
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
  fun savedPreferencesApplyWithoutOpeningSettings() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity)
        .edit()
        .putString("theme", "dark")
        .putString("language", "fr")
        .commit()

    applySavedSettings(activity)

    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    assertEquals(LocaleListCompat.forLanguageTags("fr"), AppCompatDelegate.getApplicationLocales())
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

  @Test
  fun settingsFragmentSyncsWithAppLocalesOnCreate() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment =
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            as SettingsFragment
    val preference = fragment.findPreference<DropDownPreference>("language")

    assertEquals("fr", preference?.value)

    // Reset application locales
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @Test
  fun applySavedSettingsSyncsSystemLocaleToPreferences() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity).edit().clear().commit()

    applySavedSettings(activity)

    val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
    assertEquals("en", prefs.getString("language", null))

    // Reset application locales
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @Test
  fun settingsFragmentDefaultsToSystemWhenLocalesAreEmpty() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment =
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            as SettingsFragment
    val preference = fragment.findPreference<DropDownPreference>("language")

    assertEquals("system", preference?.value)
  }

  @Test
  fun settingsFragmentReflectsNonEmptyLocalesOnCreate() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment =
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            as SettingsFragment
    val preference = fragment.findPreference<DropDownPreference>("language")

    assertEquals("en", preference?.value)
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @Test
  fun applySavedSettingsWithSystemOrDefaultLanguage() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity)
        .edit()
        .putString("language", "system")
        .commit()
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())

    applySavedSettings(activity)

    assertEquals(LocaleListCompat.getEmptyLocaleList(), AppCompatDelegate.getApplicationLocales())
  }

  @Test
  fun settingsFragmentWithNullLanguagePreference() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment = NullLanguageSettingsFragment()
    activity.supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings_container, fragment)
        .commitNow()
  }

  @Test
  fun applySavedSettingsWithUnsupportedLocale() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("de"))
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity).edit().clear().commit()

    applySavedSettings(activity)

    val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
    assertFalse(prefs.contains("language"))

    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @Test
  fun settingsFragmentWithNullLocaleLanguage() {
    mockkStatic(AppCompatDelegate::class)
    val mockLocaleList = mockk<LocaleListCompat>(relaxed = true)
    val mockLocale = mockk<Locale>(relaxed = true)
    every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleList
    every { mockLocaleList.isEmpty } returns false
    every { mockLocaleList.get(0) } returns mockLocale
    every { mockLocale.language } returns (null as String?)

    try {
      val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
      val fragment =
          activity.supportFragmentManager.findFragmentById(R.id.settings_container)
              as SettingsFragment
      val preference = fragment.findPreference<DropDownPreference>("language")
      assertEquals("system", preference?.value)
    } finally {
      unmockkStatic(AppCompatDelegate::class)
    }
  }

  @Test
  fun applySavedSettingsWithNullPreferenceString() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val mockPrefs = mockk<SharedPreferences>(relaxed = true)
    every { mockPrefs.getString("theme", "system") } returns "system"
    every { mockPrefs.contains("language") } returns true
    every { mockPrefs.getString("language", "system") } returns null

    mockkStatic(PreferenceManager::class)
    try {
      every { PreferenceManager.getDefaultSharedPreferences(any()) } returns mockPrefs
      applySavedSettings(activity)
    } finally {
      unmockkStatic(PreferenceManager::class)
    }
  }

  @Test
  fun applySavedSettingsWithNullLocaleLanguage() {
    mockkStatic(AppCompatDelegate::class)
    val mockLocaleList = mockk<LocaleListCompat>(relaxed = true)
    val mockLocale = mockk<Locale>(relaxed = true)
    every { AppCompatDelegate.getApplicationLocales() } returns mockLocaleList
    every { mockLocaleList.isEmpty } returns false
    every { mockLocaleList.get(0) } returns mockLocale
    every { mockLocale.language } returns (null as String?)

    try {
      applySavedSettings(Robolectric.buildActivity(MySettingsActivity::class.java).setup().get())
    } finally {
      unmockkStatic(AppCompatDelegate::class)
    }
  }
}

class NullLanguageSettingsFragment : SettingsFragment() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
  }
}
