// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.user

/** Value used when no name or no surname can be read from an email address. */
const val UNKNOWN_NAME = "Unknown"

/**
 * Guesses a name (first name) and a surname (family name) from the local part of [email], the part
 * before the `@`.
 *
 * Neither value is ever blank: a missing name or surname is replaced by [UNKNOWN_NAME], so the
 * result always passes the Profile screen checks.
 *
 * @param email The email address to read the names from. It may be empty or have no `@`.
 * @return The name and the surname, in this order.
 */
fun nameAndSurnameFromEmail(email: String): Pair<String, String> {
  val words =
      email
          .substringBefore('@')
          .substringBefore('+')
          .split('.', '_')
          .map(::capitalizeWord)
          .filter(String::isNotEmpty)
  val name = words.firstOrNull() ?: UNKNOWN_NAME
  val surname = words.drop(1).joinToString(" ").ifEmpty { UNKNOWN_NAME }
  return name to surname
}

/**
 * Keeps the letters, hyphens and apostrophes of [word] and capitalizes each hyphen-separated part.
 * Returns an empty string when [word] has no letter.
 */
private fun capitalizeWord(word: String): String {
  val kept = word.filter { it.isLetter() || it == '-' || it == '\'' || it == '’' }
  if (kept.none(Char::isLetter)) return ""
  return kept
      .split('-')
      .filter { part -> part.any(Char::isLetter) }
      .joinToString("-") { part -> part.lowercase().replaceFirstChar(Char::titlecase) }
}

/**
 * Returns a copy of this [User] whose name and surname are generated from its [User.email] with
 * [nameAndSurnameFromEmail], keeping any value that is already set, such as one typed in the
 * Profile screen. A blank value or [UNKNOWN_NAME] counts as not set.
 *
 * The email is read from this [User], so set it before calling this function.
 */
fun User.withNamesFromEmail(): User {
  val (generatedName, generatedSurname) = nameAndSurnameFromEmail(email)
  return copy(
      name = if (name.isBlank() || name == UNKNOWN_NAME) generatedName else name,
      surname = if (surname.isBlank() || surname == UNKNOWN_NAME) generatedSurname else surname,
  )
}
