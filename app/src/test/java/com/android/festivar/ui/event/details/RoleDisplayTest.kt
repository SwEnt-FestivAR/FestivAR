// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import com.android.festivar.model.event.role.Role
import com.android.festivar.ui.components.PillStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Unit tests of how a [Role] is shown, in RoleDisplay.kt. */
class RoleDisplayTest {

  /** Breaks if two roles share a name or a category, e.g. after a copy-pasted branch. */
  @Test
  fun everyRoleHasItsOwnNameAndCategory() {
    assertEquals(Role.entries.size, Role.entries.map { it.labelRes() }.distinct().size)
    assertEquals(Role.entries.size, Role.entries.map { it.categoryRes() }.distinct().size)
  }

  /** Breaks if a role gets a lighter badge than a role below it, or a style out of the design. */
  @Test
  fun badgesGetDarkerFromTheVisitorToTheOwner() {
    val fromLightest = listOf(PillStyle.OUTLINED, PillStyle.MUTED, PillStyle.LIME, PillStyle.INK)

    val shades = Role.entries.map { fromLightest.indexOf(it.pillStyle()) }

    assertFalse("A role has a style out of the design: $shades", -1 in shades)
    assertEquals(shades.sorted(), shades)
  }
}
