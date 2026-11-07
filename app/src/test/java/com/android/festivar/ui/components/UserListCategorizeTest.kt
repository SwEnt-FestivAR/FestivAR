// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import com.android.festivar.model.user.User
import org.junit.Assert.assertEquals
import org.junit.Test

/** The users are split into categories in the order they are shown by the [UserList]. */
class UserListCategorizeTest {

  private val unknown = "Unknown"
  private val ana = User("u1", name = "Ana")
  private val ben = User("u2", name = "Ben")
  private val cleo = User("u3", name = "Cleo")

  private fun categorize(users: List<User>, categories: Map<User, String?>) =
      categorize(users, { categories[it] }, unknown)

  /** Without categoryOf, all the users are in one category without header, in their order. */
  @Test
  fun withoutCategoryOfAllUsersAreInOneCategoryWithoutHeader() {
    assertEquals(
        listOf(UserCategory(null, listOf(ben, ana))),
        categorize(listOf(ben, ana), null, unknown),
    )
  }

  /** Categories come in the order they first appear, and users keep their order inside them. */
  @Test
  fun keepsTheOrderOfCategoriesAndOfUsers() {
    val categories = mapOf(ana to "Volunteers", ben to "Organizer", cleo to "Volunteers")

    assertEquals(
        listOf(
            UserCategory("Volunteers", listOf(ana, cleo)),
            UserCategory("Organizer", listOf(ben)),
        ),
        categorize(listOf(ana, ben, cleo), categories),
    )
  }

  /** Users without category go in the last category, even when they come first. */
  @Test
  fun usersWithoutCategoryGoInTheLastCategory() {
    assertEquals(
        listOf(UserCategory("Organizer", listOf(ben)), UserCategory(unknown, listOf(ana))),
        categorize(listOf(ana, ben), mapOf(ana to null, ben to "Organizer")),
    )
  }

  /** A category named like the last one is merged with it rather than shown twice. */
  @Test
  fun aCategoryNamedLikeTheLastOneIsMergedWithIt() {
    val categories = mapOf(ana to null, ben to unknown, cleo to "Organizer")

    assertEquals(
        listOf(UserCategory("Organizer", listOf(cleo)), UserCategory(unknown, listOf(ana, ben))),
        categorize(listOf(ana, ben, cleo), categories),
    )
  }

  /** No users give no category, except the single category without header of a plain list. */
  @Test
  fun noUsersGiveNoCategoryButOneEmptyWithoutCategoryOf() {
    assertEquals(emptyList<UserCategory>(), categorize(emptyList(), emptyMap()))
    assertEquals(listOf(UserCategory(null, emptyList())), categorize(emptyList(), null, unknown))
  }
}
