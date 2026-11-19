// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini
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
  /** Breaks if the SettingsActivity layout does not host the SettingsFragment in its container. */
  @Test
  fun settingsActivityUsesSettingsLayoutAndFragment() {
    val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

    assertNotNull(activity.findViewById(R.id.settings_container))
    assertTrue(
        activity.supportFragmentManager.findFragmentById(R.id.settings_container)
            is SettingsFragment
    )
  }

  /**
   * Breaks if onSupportNavigateUp does not finish the activity to return to the previous screen.
   */
  @Test
  fun settingsActivityNavigateUpFinishesActivity() {
    val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()

    assertTrue(activity.onSupportNavigateUp())
    assertTrue(activity.isFinishing)
  }

  /**
   * Breaks if a raw color primary value in the theme fails to fall back to typedValue.data for
   * tinting.
   */
  @Test
  fun settingsActivityWithRawColorPrimaryThemeUsesTypedValueData() {
    val controller = Robolectric.buildActivity(SettingsActivity::class.java)
    controller.get().setTheme(R.style.Theme_Test_RawColorPrimary)
    val activity = controller.setup().get()

    assertNotNull(activity.findViewById(R.id.settings_container))
  }
}
