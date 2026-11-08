// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.user

/** Value used when no name or no surname can be read from an email address. */
const val UNKNOWN_NAME = "Unknown"

/**
 * Guesses a name and a surname from the local part of [email], the part before the `@`.
 *
 * The local part is split on `.` and `_`: the first piece becomes the name and the remaining pieces
 * become the surname. A `+tag` suffix is dropped, and so is every character the Profile screen
 * refuses in a name (anything other than letters, `-`, `'` and `’`). Each word is capitalized,
 * keeping hyphens and apostrophes in compound names.
 *
 * Neither value is ever blank: a missing name or surname is replaced by [UNKNOWN_NAME], so the
 * result always passes the Profile screen checks.
 *
 * For example, `jean-luc.de_la.fontaine42@x.com` gives `Jean-Luc` and `De La Fontaine`, and
 * `ada@x.com` gives `Ada` and `Unknown`.
 *
 * @param email The email address to read the names from.
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
