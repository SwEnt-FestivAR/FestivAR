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
      applyTheme(theme)
      true
    }

    val selectedTheme =
        PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("theme", "system")
            ?: "system"
    applyTheme(selectedTheme)

    ///////////////////
    ///// Language ////
    ///////////////////
    val languagePreferences = findPreference<androidx.preference.DropDownPreference>("language")
    languagePreferences?.setOnPreferenceChangeListener { _, newValue ->
      val language = newValue as? String ?: return@setOnPreferenceChangeListener false
      applyLanguage(language)
      true
    }

    val selectedLanguage =
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString("language", "system") // the system is not magic here
        ?: "system"
    applyLanguage(selectedLanguage)
  }

  private fun applyTheme(theme: String) {
    AppCompatDelegate.setDefaultNightMode(nightModeForTheme(theme))
  }

  private fun applyLanguage(lan: String) {
    AppCompatDelegate.setApplicationLocales(localeFromString(lan))
  }
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
      else -> LocaleListCompat.getDefault()
    }
