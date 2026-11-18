// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.DropDownPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.android.festivar.R

/**
 * Fragment responsible for displaying and handling user settings such as theme (light/dark/system)
 * and language preferences (English/French/System).
 *
 * Supports two-way synchronization with Android system-level per-app language settings.
 */
open class SettingsFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)

    ///////////////////
    ////// THEME //////
    ///////////////////
    val themePreference = findPreference<DropDownPreference>("theme")
    themePreference?.setOnPreferenceChangeListener { _, newValue ->
      val theme = newValue as? String ?: return@setOnPreferenceChangeListener false
      applyThemePreference(theme)
      true
    }

    ///////////////////
    ///// Language ////
    ///////////////////
    val languagePreferences = findPreference<DropDownPreference>("language")

    // Two-way sync: reflect current active app locales (whether set in-app or via Android System
    // Settings).
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang =
        if (currentLocales.isEmpty) {
          "system"
        } else {
          currentLocales[0]?.language ?: "system"
        }
    languagePreferences?.value = currentLang

    // Listen for language preference changes and persist them to both AppCompatDelegate and
    // SharedPreferences.
    languagePreferences?.setOnPreferenceChangeListener { _, newValue ->
      val language = newValue as? String ?: return@setOnPreferenceChangeListener false
      applyLanguagePreference(language)
      PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
        putString("language", language)
      }
      true
    }
    applySavedSettings(requireContext())
  }
}

/**
 * Applies saved theme and language preferences from SharedPreferences and synchronizes OS locales.
 */
internal fun applySavedSettings(context: Context) {
  val preferences = PreferenceManager.getDefaultSharedPreferences(context)
  applyThemePreference(preferences.getString("theme", "system") ?: "system")

  // Synchronize OS-level locale changes (from Android System Settings) back to SharedPreferences.
  val currentLocales = AppCompatDelegate.getApplicationLocales()
  if (!currentLocales.isEmpty) { // Checks if android settings are default/system
    val lang = currentLocales[0]?.language
    if (lang == "fr" || lang == "en") {
      preferences.edit { putString("language", lang) }
      applyLanguagePreference(lang)
    } else {
      if (preferences.contains("language")) {
        applyLanguagePreference(preferences.getString("language", "system") ?: "system")
      }
    }
  } else {
    // When application locales are empty (system default or reset to system),
    // if SharedPreferences holds a stale stored "en" or "fr" preference, synchronize it to "system"
    // so it does not override the system setting.
    val savedLang = preferences.getString("language", "system")
    if (preferences.contains("language") && (savedLang == "fr" || savedLang == "en")) {
      preferences.edit { putString("language", "system") }
    }
    applyLanguagePreference("system")
  }
}

/** Applies the given theme string to the application night mode. */
private fun applyThemePreference(theme: String) {
  AppCompatDelegate.setDefaultNightMode(nightModeForTheme(theme))
}

/** Applies the given language string to the application locales. */
private fun applyLanguagePreference(language: String) {
  AppCompatDelegate.setApplicationLocales(localeFromString(language))
}

/**
 * Maps a theme string ("light", "dark", "system") to its corresponding AppCompatDelegate night mode
 * integer.
 */
internal fun nightModeForTheme(theme: String): Int =
    when (theme) {
      "light" -> AppCompatDelegate.MODE_NIGHT_NO
      "dark" -> AppCompatDelegate.MODE_NIGHT_YES
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

/**
 * Converts a language code string ("fr", "en") into a LocaleListCompat, or returns an empty locale
 * list for system default.
 */
internal fun localeFromString(s: String): LocaleListCompat =
    when (s) {
      "fr" -> LocaleListCompat.forLanguageTags("fr")
      "en" -> LocaleListCompat.forLanguageTags("en")
      else -> LocaleListCompat.getEmptyLocaleList()
    }
