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
  // Fails if theme strings ("light", "dark", "system", "unknown") do not map to the correct
  // AppCompatDelegate night mode integers.
  @Test
  fun themePreferenceAppliesSelectedNightMode() {
    assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeForTheme("light"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeForTheme("dark"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("system"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("unknown"))
  }

  // Fails if language code strings ("fr", "en", "system", "de") do not map to the expected
  // LocaleListCompat values or empty locale lists.
  @Test
  fun localeFromStringElseBranch() {
    assertEquals(LocaleListCompat.forLanguageTags("fr"), localeFromString("fr"))
    assertEquals(LocaleListCompat.forLanguageTags("en"), localeFromString("en"))
    assertEquals(LocaleListCompat.getEmptyLocaleList(), localeFromString("system"))
    assertEquals(LocaleListCompat.getEmptyLocaleList(), localeFromString("de"))
  }

  // Fails if theme and language helper mappings fail their expected values.
  @Test
  fun settingsFragmentMapsThemeAndLanguagePreferences() {
    assertEquals(AppCompatDelegate.MODE_NIGHT_NO, nightModeForTheme("light"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, nightModeForTheme("dark"))
    assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, nightModeForTheme("system"))
    assertEquals(LocaleListCompat.forLanguageTags("fr"), localeFromString("fr"))
    assertEquals(LocaleListCompat.forLanguageTags("en"), localeFromString("en"))
    assertEquals(LocaleListCompat.getEmptyLocaleList(), localeFromString("system"))
  }

  // Fails if changing the theme preference value in settings does not update the default night mode
  // correctly or accepts invalid non-string types.
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

  // Fails if changing the language preference value in settings does not apply the selected
  // application locale correctly or accepts invalid non-string types.
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

  // Fails if saved theme and language preferences in SharedPreferences are not applied on startup
  // when settings are not opened.
  @Test
  fun savedPreferencesApplyWithoutOpeningSettings() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity)
        .edit()
        .putString("theme", "dark")
        .putString("language", "fr")
        .commit()

    applySavedSettings(activity)

    assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    assertEquals(LocaleListCompat.forLanguageTags("fr"), AppCompatDelegate.getApplicationLocales())
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  // Fails if resetting application locales to system does not update stored language preference to
  // "system" and apply system locales.
  @Test
  fun resetToSystemClearsStoredLanguagePreferenceAndAppliesSystemLocale() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity)
        .edit()
        .putString("language", "fr")
        .commit()
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())

    applySavedSettings(activity)

    val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
    assertEquals("system", prefs.getString("language", null))
    assertEquals(LocaleListCompat.getEmptyLocaleList(), AppCompatDelegate.getApplicationLocales())
  }

  // Fails if settings fragment crashes or fails to initialize when SharedPreferences are empty or
  // null.
  @Test
  fun settingsFragmentWithNullPreferencesInSharedPreferences() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    PreferenceManager.getDefaultSharedPreferences(activity).edit().clear().commit()

    val fragment =
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            as SettingsFragment
    fragment.onCreatePreferences(null, null)
  }

  // Fails if the settings fragment language preference value does not reflect the current active
  // app
  // locales on creation.
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

  // Fails if applying saved settings does not synchronize active OS-level app locales back to
  // SharedPreferences.
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

  // Fails if the settings fragment language preference does not default to "system" when
  // application
  // locales are empty.
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

  // Fails if the settings fragment language preference does not reflect non-empty application
  // locales on creation.
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

  // Fails if applying saved settings with system/default language preference fails to keep
  // application locales empty.
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

  // Fails if the settings fragment fails to initialize when the language preference key is missing
  // or
  // null.
  @Test
  fun settingsFragmentWithNullLanguagePreference() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()
    val fragment = NullLanguageSettingsFragment()
    activity.supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings_container, fragment)
        .commitNow()
  }

  // Fails if applying saved settings with an unsupported locale incorrectly persists the
  // unsupported
  // language tag to SharedPreferences.
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

  // Fails if settings fragment crashes when a locale has a null language code.
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

  // Fails if applySavedSettings throws an exception when SharedPreferences returns a null
  // preference
  // string.
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

  // Fails if applySavedSettings crashes when encountering a null locale language during OS locale
  // synchronization.
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
