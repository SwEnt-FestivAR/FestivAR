package com.android.festivar.ui.authentication

import java.util.regex.Pattern

private const val MIN_PASSWORD_LENGTH = 8

private val EMAIL_ADDRESS =
    Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
    )

/**
 * Checks if the provided email is valid.
 *
 * @param email The email to check.
 * @return True if the email is valid, false otherwise.
 */
fun isEmailValid(email: String): Boolean {
  return email.isNotBlank() && EMAIL_ADDRESS.matcher(email).matches()
}

/**
 * Checks if the provided password is valid.
 *
 * @param password The password to check.
 * @return A null string if the password is valid, otherwise an error message.
 */
fun checkPassword(password: String): String? {
  if (password.length >= MIN_PASSWORD_LENGTH) return null
  return "Password must be at least $MIN_PASSWORD_LENGTH characters."
}

/**
 * Checks if the provided password is valid.
 *
 * @param password The password to check.
 * @return True if the password is valid, false otherwise.
 */
fun isPasswordValid(password: String): Boolean {
  return checkPassword(password) == null
}
