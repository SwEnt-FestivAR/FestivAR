// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameFromEmailTest {

  @Test
  fun splitsNameAndSurnameOnDot() {
    assertEquals("Salim" to "Chaoui", nameAndSurnameFromEmail("salim.chaoui@gmail.com"))
  }

  @Test
  fun splitsOnUnderscoreAndJoinsSurnameWords() {
    assertEquals(
        "Jean-Luc" to "De La Fontaine",
        nameAndSurnameFromEmail("jean-luc.de_la.fontaine@example.com"),
    )
  }

  @Test
  fun capitalizesEachWordAndLowersTheRest() {
    assertEquals("Ada" to "Lovelace", nameAndSurnameFromEmail("ADA.loveLACE@example.com"))
  }

  @Test
  fun usesUnknownSurnameWhenLocalPartHasOneWord() {
    assertEquals("Ada" to UNKNOWN_NAME, nameAndSurnameFromEmail("ada@example.com"))
  }

  @Test
  fun dropsPlusTagAndDigits() {
    assertEquals("Ada" to "Lovelace", nameAndSurnameFromEmail("ada.lovelace42+news@example.com"))
  }

  @Test
  fun keepsApostrophes() {
    assertEquals("Sean" to "O'neil", nameAndSurnameFromEmail("sean.o'neil@example.com"))
  }

  @Test
  fun skipsWordsWithoutLetters() {
    assertEquals("Ada" to "Lovelace", nameAndSurnameFromEmail("123.ada..-.lovelace@example.com"))
  }

  @Test
  fun dropsHyphensWithoutLettersAround() {
    assertEquals("Jean-Luc" to UNKNOWN_NAME, nameAndSurnameFromEmail("-jean--luc-@example.com"))
  }

  @Test
  fun usesUnknownForBothWhenNoLetter() {
    assertEquals(UNKNOWN_NAME to UNKNOWN_NAME, nameAndSurnameFromEmail("1234@example.com"))
  }

  @Test
  fun usesUnknownForBothWhenEmailIsEmpty() {
    assertEquals(UNKNOWN_NAME to UNKNOWN_NAME, nameAndSurnameFromEmail(""))
  }

  @Test
  fun usesWholeInputWhenThereIsNoAt() {
    assertEquals("Ada" to "Lovelace", nameAndSurnameFromEmail("ada.lovelace"))
  }

  @Test
  fun usesTheWholeWordWhenThereIsNoSeparator() {
    assertEquals(
        "Pokemonpokemon" to UNKNOWN_NAME,
        nameAndSurnameFromEmail("pokemonpokemon@gmail.com"),
    )
  }

  @Test
  fun putsEveryWordAfterTheFirstInTheSurname() {
    assertEquals("Abcd" to "Abcd Youyou", nameAndSurnameFromEmail("abcd.abcd.youyou@gmail.com"))
  }

  @Test
  fun keepsSingleLetterWords() {
    assertEquals("J" to "D", nameAndSurnameFromEmail("j.d@example.com"))
  }

  @Test
  fun joinsWordsSplitOnlyByDigits() {
    assertEquals("Johndoe" to UNKNOWN_NAME, nameAndSurnameFromEmail("john2doe@example.com"))
  }

  @Test
  fun keepsAccentedLetters() {
    assertEquals("Élodie" to "Müller", nameAndSurnameFromEmail("élodie.müller@example.com"))
  }

  @Test
  fun usesUnknownForBothWhenLocalPartIsEmpty() {
    assertEquals(UNKNOWN_NAME to UNKNOWN_NAME, nameAndSurnameFromEmail("@example.com"))
    assertEquals(UNKNOWN_NAME to UNKNOWN_NAME, nameAndSurnameFromEmail("+news@example.com"))
  }

  @Test
  fun withNamesFromEmail_fillsBlankNames() {
    val user = User(uid = UID, email = "ada.lovelace@example.com")

    assertEquals(
        user.copy(name = "Ada", surname = "Lovelace"),
        user.withNamesFromEmail(),
    )
  }

  @Test
  fun withNamesFromEmail_replacesUnknownNames() {
    val user =
        User(
            uid = UID,
            name = UNKNOWN_NAME,
            surname = UNKNOWN_NAME,
            email = "ada.lovelace@example.com",
        )

    assertEquals(user.copy(name = "Ada", surname = "Lovelace"), user.withNamesFromEmail())
  }

  @Test
  fun withNamesFromEmail_keepsTypedNames() {
    val user =
        User(uid = UID, name = "Grace", surname = "Hopper", email = "ada.lovelace@example.com")

    assertEquals(user, user.withNamesFromEmail())
  }

  @Test
  fun withNamesFromEmail_onlyFillsTheMissingValue() {
    val typedName =
        User(uid = UID, name = "Grace", surname = "", email = "ada.lovelace@example.com")
    val typedSurname =
        User(uid = UID, name = UNKNOWN_NAME, surname = "Hopper", email = "ada.lovelace@example.com")

    assertEquals(typedName.copy(surname = "Lovelace"), typedName.withNamesFromEmail())
    assertEquals(typedSurname.copy(name = "Ada"), typedSurname.withNamesFromEmail())
  }

  @Test
  fun withNamesFromEmail_usesUnknownWhenEmailIsEmpty() {
    val user = User(uid = UID)

    assertEquals(
        user.copy(name = UNKNOWN_NAME, surname = UNKNOWN_NAME),
        user.withNamesFromEmail(),
    )
  }

  @Test
  fun withNamesFromEmail_keepsOtherFields() {
    val user =
        User(
            uid = UID,
            email = "ada.lovelace@example.com",
            joinedEvents = listOf("event-1"),
            tasksAssigned = listOf("task-1"),
        )

    val named = user.withNamesFromEmail()

    assertEquals(user.joinedEvents, named.joinedEvents)
    assertEquals(user.tasksAssigned, named.tasksAssigned)
    assertEquals(user.email, named.email)
    assertEquals(user.uid, named.uid)
  }

  @Test
  fun resultOnlyContainsCharactersAllowedByTheProfileScreen() {
    val (name, surname) = nameAndSurnameFromEmail("é.l'a_m-b😀d#a.9@example.com")

    listOf(name, surname).forEach { value ->
      assertTrue(value.isNotBlank())
      assertTrue(value.any(Char::isLetter))
      assertTrue(value.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' || it == '’' })
    }
  }

  private companion object {
    const val UID = "name-from-email-uid"
  }
}
