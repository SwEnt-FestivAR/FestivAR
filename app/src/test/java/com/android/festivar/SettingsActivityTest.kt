// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.settings.SettingsFragment
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {
  @Test
  fun settingsActivityUsesSettingsLayoutAndFragment() {
    val activity = Robolectric.buildActivity(MySettingsActivity::class.java).setup().get()

    assertNotNull(activity.findViewById(R.id.settings_container))
    assertTrue(
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            is SettingsFragment
    )
  }
}
