// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.temporary

import org.junit.Assert.assertNotEquals
import org.junit.Test

class UserTest {

  /**
   * Users are compared on every field, not only their uid, so that a change of name or surname is
   * seen as a change (e.g. by a StateFlow). Events and tasks identify their users by uid instead.
   */
  @Test
  fun usersWithTheSameUidButOtherNamesAreNotEqual() {
    val sara = User("u1", name = "Sara", surname = "Keller")

    assertNotEquals(sara, User("u1"))
    assertNotEquals(sara, sara.copy(name = "Sarah"))
    assertNotEquals(sara, sara.copy(surname = "Muller"))
  }
}
