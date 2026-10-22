package com.android.festivar.ui.authentication

const val MIN_PASSWORD_LENGTH = 8

/**
 * Checks if the provided email is valid.
 *
 * @param email The email to check.
 * @return True if the email is valid, false otherwise.
 */
fun isEmailValid(email: String): Boolean {
  return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

/**
 * Checks if the provided password is valid.
 *
 * @param password The password to check.
 * @return An empty string if the password is valid, otherwise an error message.
 */
fun checkPassword(password: String): String {
  if (password.length >= MIN_PASSWORD_LENGTH) return ""
  return "Password must be at least $MIN_PASSWORD_LENGTH characters."
}

/**
 * Checks if the provided password is valid.
 *
 * @param password The password to check.
 * @return True if the password is valid, false otherwise.
 */
fun isPasswordValid(password: String): Boolean {
  return checkPassword(password) == ""
}
