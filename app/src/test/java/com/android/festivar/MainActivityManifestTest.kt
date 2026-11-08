// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Pins the manifest flags of [MainActivity] that the screens rely on. */
@RunWith(AndroidJUnit4::class)
class MainActivityManifestTest {

  /**
   * Without `adjustResize` the soft keyboard covers the bottom of a screen and a scrolling column
   * has nothing to scroll, so the Join buttons of the join event screen cannot be reached.
   */
  // The flag is deprecated from API 30 for edge-to-edge apps; this app targets SDK 34 and draws
  // behind no system bar, so the window manager still honours it.
  @Suppress("DEPRECATION")
  @Test
  fun theSoftKeyboardResizesTheMainActivityWindow() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val info =
        context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

    assertEquals(SOFT_INPUT_ADJUST_RESIZE, info.softInputMode and SOFT_INPUT_MASK_ADJUST)
  }

  /**
   * The Maps SDK reads its key from this meta-data; without the entry a map crashes at start. The
   * value is the placeholder DEFAULT_API_KEY unless local.properties holds a real key.
   */
  @Test
  fun theMapsApiKeyIsDeclaredInTheManifest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val info =
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

    val key = info.metaData?.getString("com.google.android.geo.API_KEY")
    assertFalse("the Maps API key meta-data is missing", key.isNullOrBlank())
  }
}
