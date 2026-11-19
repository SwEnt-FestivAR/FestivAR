package com.android.festivar

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.festivar.ui.settings.SettingsFragment

class MySettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Source - https://stackoverflow.com/q/73598309
    // Posted by Alix Blaine
    // Retrieved 2026-11-05, License - CC BY-SA 4.0

    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

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
