// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.android.festivar.R

class MySettingsFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)

    val themePreference = findPreference<androidx.preference.ListPreference>("theme")
    themePreference?.setOnPreferenceChangeListener { _, newValue ->
      val theme = newValue as? String ?: return@setOnPreferenceChangeListener false
      applyTheme(theme)
      true
    }

    val selectedTheme =
        PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("theme", "system")
            ?: "system"
    applyTheme(selectedTheme)
  }

  private fun applyTheme(theme: String) {
    AppCompatDelegate.setDefaultNightMode(nightModeForTheme(theme))
  }
}

internal fun nightModeForTheme(theme: String): Int =
    when (theme) {
      "light" -> AppCompatDelegate.MODE_NIGHT_NO
      "dark" -> AppCompatDelegate.MODE_NIGHT_YES
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
