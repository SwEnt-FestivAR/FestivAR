// Co-authored-by: Gemini
package com.android.festivar.model.authentication

/**
 * Masks an email ex: skibidi@toilet.com -> sk*****@toilet.com
 *
 * @param email The email to mask
 * @return The masked email
 */
fun maskEmail(email: String): String {
  val parts = email.split("@")
  if (parts.size != 2) return email
  val local = parts[0]
  val domain = parts[1]
  val maskedLocal =
      if (local.length <= 2) {
        local
      } else {
        local.take(2) + "*".repeat(local.length - 2)
      }
  return "$maskedLocal@$domain"
}
