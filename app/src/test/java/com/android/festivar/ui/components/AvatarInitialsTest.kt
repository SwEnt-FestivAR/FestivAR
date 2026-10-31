// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import com.android.festivar.model.user.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The initials are built only from the letters of the name and surname. The usual cases (Sara
 * Keller, a single name, blank names) are checked on the drawn avatar in `AvatarTest`.
 */
class AvatarInitialsTest {

  /** A hyphen or an apostrophe before the first letter is skipped. */
  @Test
  fun skipsNonLettersAtTheStart() {
    assertEquals("AL", initials(User("u1", name = "-Ana", surname = "'Lou")))
  }

  /** Digits are not letters, so the initials are the first two letters after them. */
  @Test
  fun skipsDigits() {
    assertEquals("EV", initials(User("u1", name = "4ever")))
  }

  /** A name without any letter counts as no name, so the surname gives both initials. */
  @Test
  fun aNameWithoutLettersFallsBackToTheSurname() {
    assertEquals("KE", initials(User("u1", name = "123", surname = "Keller")))
  }

  /** Punctuation or an emoji alone gives no initials, so the avatar shows the person icon. */
  @Test
  fun noLetterAtAllGivesNoInitials() {
    assertNull(initials(User("u1", name = "!!")))
    assertNull(initials(User("u1", name = "😀")))
  }

  /** Accented letters are letters, and they are kept and uppercased. */
  @Test
  fun keepsAccentedLetters() {
    assertEquals("ÉL", initials(User("u1", name = "élodie")))
  }
}
