// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformTest {

  /** Breaks if the number of bars depends on how many samples were taken. */
  @Test
  fun downsampleWaveform_alwaysGivesTheFixedNumberOfBars() {
    listOf(1, 5, WAVEFORM_BAR_COUNT, 41, 1_000).forEach { count ->
      assertEquals(WAVEFORM_BAR_COUNT, downsampleWaveform(List(count) { 100 }).size)
    }
  }

  /** Breaks if a bar leaves the range 0 to 1, for the quietest and the loudest amplitude. */
  @Test
  fun downsampleWaveform_keepsEveryBarBetweenZeroAndOne() {
    val bars = downsampleWaveform(List(100) { if (it % 2 == 0) 0 else MAX_AMPLITUDE })

    assertTrue(bars.all { it in 0f..1f })
    assertEquals(1f, bars.max(), 0f)
  }

  /** Breaks if a silent recording gives a bar above zero or fewer bars than the others. */
  @Test
  fun downsampleWaveform_givesSilentBarsForSilence() {
    assertEquals(List(WAVEFORM_BAR_COUNT) { 0f }, downsampleWaveform(List(80) { 0 }))
  }

  /** Breaks if a recording without any sample looks like silence instead of an unknown level. */
  @Test
  fun downsampleWaveform_givesNoBarForNoSample() {
    assertEquals(emptyList<Float>(), downsampleWaveform(emptyList()))
  }

  /** Breaks if a short loud sound is averaged away instead of staying visible in its bar. */
  @Test
  fun downsampleWaveform_keepsTheLoudestSampleOfEachBar() {
    val samples = MutableList(WAVEFORM_BAR_COUNT * 2) { 0 }
    samples[3] = MAX_AMPLITUDE

    val bars = downsampleWaveform(samples)

    assertEquals(0f, bars[0], 0f)
    assertEquals(1f, bars[1], 0f)
    assertEquals(0f, bars[2], 0f)
  }

  /** Breaks if an amplitude above the maximum gives a bar above 1, which would lose the note. */
  @Test
  fun downsampleWaveform_clampsAnAmplitudeAboveTheMaximum() {
    val bars = downsampleWaveform(List(WAVEFORM_BAR_COUNT) { MAX_AMPLITUDE + 1 })

    assertEquals(List(WAVEFORM_BAR_COUNT) { 1f }, bars)
  }

  /** Breaks if the bars no longer follow the recording in time order. */
  @Test
  fun downsampleWaveform_keepsTheOrderOfTheRecording() {
    val bars = downsampleWaveform(List(WAVEFORM_BAR_COUNT) { it * 100 })

    assertEquals(bars.sorted(), bars)
    assertEquals(0f, bars.first(), 0f)
  }

  /** Breaks if fewer samples than bars leave a bar out instead of repeating a sample. */
  @Test
  fun downsampleWaveform_withFewSamples_repeatsThemInOrder() {
    val bars = downsampleWaveform(listOf(0, MAX_AMPLITUDE))

    assertEquals(0f, bars.first(), 0f)
    assertEquals(1f, bars.last(), 0f)
  }
}
