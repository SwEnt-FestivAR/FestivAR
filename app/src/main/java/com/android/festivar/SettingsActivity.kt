package com.android.festivar

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.festivar.ui.settings.SettingsFragment

class MySettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings_container, SettingsFragment())
        .commit()
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return true
  }
}
