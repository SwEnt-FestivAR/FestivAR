// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import kotlinx.coroutines.delay

/** Whether this phone can show AR, in the app's own words. */
enum class ArAvailability {
  /** The phone runs AR and Google Play Services for AR is installed and recent enough. */
  SUPPORTED,

  /**
   * The phone can run AR but Google Play Services for AR is missing or too old. The AR session asks
   * for the install before it starts.
   */
  NEEDS_INSTALL,

  /** The phone cannot run AR. */
  UNSUPPORTED,

  /**
   * The check could not tell, for instance because the phone was offline or ARCore never settled.
   * Asking again later may give another answer.
   */
  UNKNOWN,
}

/** Tells whether this phone can show AR. */
fun interface ArSupport {
  /** Answers whether this phone can show AR. It may take a moment. */
  suspend fun check(): ArAvailability
}

/** How many times ARCore is asked while it answers that it is still checking. */
internal const val ARCORE_CHECK_ATTEMPTS = 10

/** How long to wait between two questions to ARCore, in milliseconds. */
private const val ARCORE_CHECK_DELAY_MS = 200L

/**
 * The [ArSupport] of a real phone, over `ArCoreApk.checkAvailability`.
 *
 * It holds nothing but the call to ARCore, because ARCore cannot run in a unit test: the waiting
 * and the translation live in [pollArCore], which is tested on its own.
 *
 * It keeps the application context only, so building it from a screen does not keep that screen
 * alive.
 */
class ArCoreSupport(context: Context) : ArSupport {
  private val context = context.applicationContext

  override suspend fun check(): ArAvailability = pollArCore {
    ArCoreApk.getInstance().checkAvailability(context)
  }
}

/**
 * Asks [availability] until it stops answering `UNKNOWN_CHECKING`, the one transient answer ARCore
 * gives while it queries its servers, waiting 200 milliseconds between two questions. After
 * [ARCORE_CHECK_ATTEMPTS] questions still checking, the answer is [ArAvailability.UNKNOWN].
 * Installed is [ArAvailability.SUPPORTED]; missing or too old is [ArAvailability.NEEDS_INSTALL];
 * not capable is [ArAvailability.UNSUPPORTED], the only answer saying the phone cannot do AR; an
 * error or a timeout is [ArAvailability.UNKNOWN].
 */
internal suspend fun pollArCore(availability: () -> Availability): ArAvailability {
  repeat(ARCORE_CHECK_ATTEMPTS) { attempt ->
    when (availability()) {
      Availability.SUPPORTED_INSTALLED -> return ArAvailability.SUPPORTED
      Availability.SUPPORTED_NOT_INSTALLED,
      Availability.SUPPORTED_APK_TOO_OLD -> return ArAvailability.NEEDS_INSTALL
      Availability.UNKNOWN_CHECKING ->
          if (attempt < ARCORE_CHECK_ATTEMPTS - 1) delay(ARCORE_CHECK_DELAY_MS)
      Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> return ArAvailability.UNSUPPORTED
      else -> return ArAvailability.UNKNOWN
    }
  }
  return ArAvailability.UNKNOWN
}
