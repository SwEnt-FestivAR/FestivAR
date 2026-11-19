// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

/** How many bars the waveform of a voice note has, whatever the length of the recording. */
const val WAVEFORM_BAR_COUNT = 40

/**
 * The loudest value [android.media.MediaRecorder.getMaxAmplitude] can return: it reads 16-bit
 * samples.
 */
internal const val MAX_AMPLITUDE = Short.MAX_VALUE.toInt()

/**
 * Reduces the [amplitudes] sampled during a recording to exactly [WAVEFORM_BAR_COUNT] bars between
 * 0 and 1. A bar is the loudest sample of its share of the recording, so a short loud sound stays
 * visible. With fewer samples than bars, neighbouring bars repeat a sample. No sample at all, as
 * when the level could never be read, gives no bars, so that it is not mistaken for silence.
 */
internal fun toWaveformBars(amplitudes: List<Int>): List<Float> {
  if (amplitudes.isEmpty()) return emptyList()

  return List(WAVEFORM_BAR_COUNT) { bar ->
    val loudest = readingsOfBar(bar, amplitudes).max()
    // The clamp is needed: a device may report 32768, and a bar above 1 would make VoiceNote refuse
    // the note after its audio is uploaded, losing it.
    (loudest.toFloat() / MAX_AMPLITUDE).coerceIn(0f, 1f)
  }
}

/** The readings that the bar number [bar] summarises: its share of [amplitudes], at least one. */
private fun readingsOfBar(bar: Int, amplitudes: List<Int>): List<Int> {
  val from = bar * amplitudes.size / WAVEFORM_BAR_COUNT
  val to = maxOf(from + 1, (bar + 1) * amplitudes.size / WAVEFORM_BAR_COUNT)
  return amplitudes.subList(from, to)
}
