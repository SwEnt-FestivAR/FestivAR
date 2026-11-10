package com.android.festivar.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.android.festivar.R

class MySettingsFragment : PreferenceFragmentCompat() {
  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)
  }
}
