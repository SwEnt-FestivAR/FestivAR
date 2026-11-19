// Co-authored-by: Gemini <noreply@google.com>
package com.android.festivar

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.android.festivar.ui.settings.SettingsFragment

class SettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Source - https://stackoverflow.com/q/73598309
    // Posted by Alix Blaine
    // Retrieved 2026-11-05, License - CC BY-SA 4.0

    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    setContentView(R.layout.activity_settings)

    // 1. Set up the Toolbar as the ActionBar
    val toolbar = findViewById<Toolbar>(R.id.settings_toolbar)
    setSupportActionBar(toolbar)

    // 2. Enable the back arrow
    val actionBar = requireNotNull(supportActionBar)
    actionBar.setDisplayHomeAsUpEnabled(true)
    actionBar.setDisplayShowHomeEnabled(true)

    val typedValue = TypedValue()
    theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    val color =
        if (typedValue.resourceId != 0) {
          ContextCompat.getColor(this, typedValue.resourceId)
        } else {
          typedValue.data
        }
    toolbar.navigationIcon?.setTint(color)

    // Optional: Explicitly set title if needed (otherwise fetched from Manifest label)
    // supportActionBar?.title = getString(R.string.title_activity_settings)

    supportFragmentManager
        .beginTransaction()
        .replace(R.id.settings_container, SettingsFragment())
        .commit()
  }

  override fun onSupportNavigateUp(): Boolean {
    finish() // Closes the settings activity and returns to the previous screen
    return true
  }
}
