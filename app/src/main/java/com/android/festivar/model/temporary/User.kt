package com.android.festivar.model.temporary

/**
 * Temporary data class representing a [User] of the FestivAR application. The implementation and
 * parameters of this class are up to changes and this definition states only as a preliminary
 * definition to allow the implementation of other classes.
 *
 * @param uid The [User]'s unique identifier.
 * @param name The [User]'s name.
 * @param surname The [User]'s surname.
 */
data class User(val uid: String, val name: String = "", val surname: String = "")
