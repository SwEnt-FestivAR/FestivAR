// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import com.android.festivar.model.task.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests of [matchesQuery], the search rule of the task lists. */
class TaskSearchTest {
  private val task =
      Task(
          taskId = "t",
          eventId = "e",
          title = "Run power to stage",
          description = "Bring the long cables",
          location = "Main marquee",
          ownerId = "owner-1",
      )

  // Breaks if an empty query hides tasks instead of showing them all.
  @Test
  fun anEmptyQueryMatchesEveryTask() {
    assertTrue(task.matchesQuery(""))
  }

  // Breaks if a query of spaces, tabs or line breaks is searched as is instead of ignored.
  @Test
  fun aBlankQueryMatchesEveryTask() {
    assertTrue(task.matchesQuery("   "))
    assertTrue(task.matchesQuery("\t\n "))
  }

  // Breaks if the title is not searched.
  @Test
  fun aPartOfTheTitleMatches() {
    assertTrue(task.matchesQuery("power"))
  }

  // Breaks if the title must be typed in full or only its start is searched.
  @Test
  fun theWholeTitleAndItsEndMatch() {
    assertTrue(task.matchesQuery("Run power to stage"))
    assertTrue(task.matchesQuery("stage"))
  }

  // Breaks if the location is not searched.
  @Test
  fun aPartOfTheLocationMatches() {
    assertTrue(task.matchesQuery("marquee"))
  }

  // Breaks if the case of the query or of the task matters.
  @Test
  fun theCaseIsIgnored() {
    assertTrue(task.matchesQuery("POWER"))
    assertTrue(task.matchesQuery("mAiN"))
  }

  // Breaks if the case of letters outside ASCII matters.
  @Test
  fun theCaseOfAccentedLettersIsIgnored() {
    val fete = task.copy(title = "Fête de la musique")

    assertTrue(fete.matchesQuery("FÊTE"))
  }

  // Breaks if the spaces around the query are searched too.
  @Test
  fun theSpacesAroundTheQueryAreIgnored() {
    assertTrue(task.matchesQuery("  power "))
  }

  // Breaks if the spaces inside the query are dropped or merged.
  @Test
  fun theSpacesInsideTheQueryAreKept() {
    assertTrue(task.matchesQuery("power to"))
    assertFalse(task.matchesQuery("power  to"))
  }

  // Breaks if a query found in neither the title nor the location still matches.
  @Test
  fun aQueryFoundNowhereDoesNotMatch() {
    assertFalse(task.matchesQuery("bar"))
  }

  // Breaks if the description is searched too.
  @Test
  fun theDescriptionIsNotSearched() {
    assertFalse(task.matchesQuery("cables"))
  }

  // Breaks if the title and the location are searched as one text joined together.
  @Test
  fun aQueryAcrossTheTitleAndTheLocationDoesNotMatch() {
    assertFalse(task.matchesQuery("stage Main"))
  }

  // Breaks if a task without a location matches a query found in no title.
  @Test
  fun aTaskWithoutALocationMatchesOnlyByItsTitle() {
    val noLocation = task.copy(location = "")

    assertTrue(noLocation.matchesQuery("power"))
    assertFalse(noLocation.matchesQuery("marquee"))
  }

  // Breaks if the query is read as a regular expression instead of plain text.
  @Test
  fun theQueryIsPlainText() {
    val withBrackets = task.copy(title = "Stage (A) check")

    assertTrue(withBrackets.matchesQuery("(A)"))
    assertFalse(task.matchesQuery(".*"))
  }
}
