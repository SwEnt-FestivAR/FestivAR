// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.android.festivar.R

class SettingsFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)

    ///////////////////
    ////// THEME //////
    ///////////////////
    val themePreference = findPreference<androidx.preference.DropDownPreference>("theme")
    themePreference?.setOnPreferenceChangeListener { _, newValue ->
      val theme = newValue as? String ?: return@setOnPreferenceChangeListener false
      applyThemePreference(theme)
      true
    }

    ///////////////////
    ///// Language ////
    ///////////////////
    val languagePreferences = findPreference<androidx.preference.DropDownPreference>("language")
    languagePreferences?.setOnPreferenceChangeListener { _, newValue ->
      val language = newValue as? String ?: return@setOnPreferenceChangeListener false
      applyLanguagePreference(language)
      true
    }
    applySavedSettings(requireContext())
  }
}

internal fun applySavedSettings(context: android.content.Context) {
  val preferences = PreferenceManager.getDefaultSharedPreferences(context)
  applyThemePreference(preferences.getString("theme", "system") ?: "system")
  applyLanguagePreference(preferences.getString("language", "system") ?: "system")
}

private fun applyThemePreference(theme: String) {
  AppCompatDelegate.setDefaultNightMode(nightModeForTheme(theme))
}

private fun applyLanguagePreference(language: String) {
  AppCompatDelegate.setApplicationLocales(localeFromString(language))
}

internal fun nightModeForTheme(theme: String): Int =
    when (theme) {
      "light" -> AppCompatDelegate.MODE_NIGHT_NO
      "dark" -> AppCompatDelegate.MODE_NIGHT_YES
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

internal fun localeFromString(s: String): LocaleListCompat =
    when (s) {
      "fr" -> LocaleListCompat.forLanguageTags("fr")
      "en" -> LocaleListCompat.forLanguageTags("en")
      else -> LocaleListCompat.getEmptyLocaleList()
    }
