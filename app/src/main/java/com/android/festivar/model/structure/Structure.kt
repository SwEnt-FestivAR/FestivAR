// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

/** What an organizer can place on the venue plan. [CUSTOM] carries its own label and size. */
enum class StructureKind {
  MARQUEE,
  STAGE,
  STAND,
  TABLE,
  CUSTOM,
}

/**
 * Represents a [Structure] in the FestivAR application: a footprint drawn on the venue plan of an
 * [Event], in metres from the venue centre, x east and y south. Organizers lay out the venue with
 * structures (marquee, stage, stand, table, or a custom one with its own name and size).
 *
 * A [Structure] is immutable: its fields can never be changed. Any change goes through [copy],
 * which returns a new [Structure], so the result must be kept as: `s = s.copy(x = 3.0)`.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [structureId] is not blank;
 * - [eventId] is not blank;
 * - [label] is not blank;
 * - [width] and [height] are above 0.
 *
 * @property structureId Unique identifier of the [Structure].
 * @property eventId Identifier of the [Event] whose venue plan the [Structure] is drawn on.
 * @property kind What the [Structure] is.
 * @property label Name of the [Structure], shown on the plan.
 * @property x Metres east of the venue centre to the footprint's left edge.
 * @property y Metres south of the venue centre to the footprint's top edge.
 * @property width East to west size in metres.
 * @property height North to south size in metres.
 */
data class Structure(
    val structureId: String,
    val eventId: String,
    val kind: StructureKind,
    val label: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {

  init {
    require(structureId.isNotBlank()) { "The structureId cannot be empty." }
    require(eventId.isNotBlank()) { "The eventId cannot be empty." }
    require(label.isNotBlank()) { "The label cannot be empty." }
    require(width > 0) { "The width must be above 0." }
    require(height > 0) { "The height must be above 0." }
  }

  /** Metres east of the venue centre to the middle of the footprint. */
  val centerX: Double
    get() = x + width / 2

  /** Metres south of the venue centre to the middle of the footprint. */
  val centerY: Double
    get() = y + height / 2
}
