// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.w3c.dom.Element

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
   * Breaks if the Maps API key meta-data leaves the manifest or is left empty: the Maps SDK reads
   * its key there, and a map crashes at start without it. The value is the key of local.properties
   * when there is one and the placeholder DEFAULT_API_KEY otherwise, so this cannot tell whether
   * the secrets plugin works; the APK workflow checks that.
   */
  @Test
  fun theMapsApiKeyIsDeclaredInTheManifest() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val info =
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

    val key = info.metaData?.getString("com.google.android.geo.API_KEY")
    assertFalse("the Maps API key meta-data is missing", key.isNullOrBlank())
  }

  /**
   * Breaks if the app's own manifest stops declaring ARCore optional, or the merged manifest says
   * anything else: with "required" the Play Store hides the app from every phone without AR. The
   * SceneView manifest says "optional" today, so only the source check sees our entry go.
   */
  @Test
  fun arCoreIsDeclaredOptional() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val info =
        context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

    assertEquals("optional", info.metaData?.getString("com.google.ar.core"))
    assertEquals(
        "optional",
        appManifestElement("meta-data", "com.google.ar.core")?.getAttribute("android:value"),
    )
  }

  /**
   * Breaks if the app's own manifest stops asking for the camera permission, which AR needs; the
   * SceneView manifest asks for it today, so only the source check sees our entry go.
   */
  @Test
  fun theCameraPermissionIsDeclared() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val info =
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )

    assertTrue(Manifest.permission.CAMERA in info.requestedPermissions.orEmpty())
    assertNotNull(appManifestElement("uses-permission", Manifest.permission.CAMERA))
  }

  /**
   * Breaks if the camera or the AR camera becomes a required feature, or its optional declaration
   * goes: the camera permission alone makes Play treat the camera as required, and the app would no
   * longer install on a phone without one.
   */
  @Test
  fun theCameraAndArFeaturesAreDeclaredNotRequired() {
    listOf(PackageManager.FEATURE_CAMERA, PackageManager.FEATURE_CAMERA_AR).forEach { feature ->
      val element = appManifestElement("uses-feature", feature)
      assertNotNull("$feature is not declared", element)
      assertEquals("false", element?.getAttribute("android:required"))
    }
  }

  /** The first element named [tag] with `android:name` [name] in the app's own manifest. */
  private fun appManifestElement(tag: String, name: String): Element? {
    val document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))
    val nodes = document.getElementsByTagName(tag)
    return (0 until nodes.length)
        .map { nodes.item(it) as Element }
        .firstOrNull { it.getAttribute("android:name") == name }
  }
}
