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

  // Breaks if an empty query, or one of spaces, tabs or line breaks, hides tasks.
  @Test
  fun aBlankQueryMatchesEveryTask() {
    assertTrue(task.matchesQuery(""))
    assertTrue(task.matchesQuery("\t\n "))
  }

  // Breaks if only the start of the title, or only the whole title, is searched.
  @Test
  fun anyPartOfTheTitleMatches() {
    assertTrue(task.matchesQuery("power"))
    assertTrue(task.matchesQuery("stage"))
    assertTrue(task.matchesQuery("Run power to stage"))
  }

  // Breaks if the location is not searched.
  @Test
  fun aPartOfTheLocationMatches() {
    assertTrue(task.matchesQuery("marquee"))
  }

  // Breaks if the case matters, accented letters included.
  @Test
  fun theCaseIsIgnored() {
    assertTrue(task.matchesQuery("POWER"))
    assertTrue(task.matchesQuery("mAiN"))
    assertTrue(task.copy(title = "Fête de la musique").matchesQuery("FÊTE"))
  }

  // Breaks if the spaces around the query are searched, or the ones inside dropped or merged.
  @Test
  fun onlyTheSpacesAroundTheQueryAreIgnored() {
    assertTrue(task.matchesQuery("  power "))
    assertFalse(task.matchesQuery("power  to"))
  }

  // Breaks if a text found nowhere, or only in the description, matches, or if the title and the
  // location are searched as one text joined together.
  @Test
  fun aQueryOutsideTheTitleAndTheLocationDoesNotMatch() {
    assertFalse(task.matchesQuery("bar"))
    assertFalse(task.matchesQuery("cables"))
    assertFalse(task.matchesQuery("stage Main"))
  }

  // Breaks if the query is read as a regular expression instead of plain text.
  @Test
  fun theQueryIsPlainText() {
    assertTrue(task.copy(title = "Stage (A) check").matchesQuery("(A)"))
    assertFalse(task.matchesQuery(".*"))
  }
}
