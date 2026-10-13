// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import java.util.UUID

/**
 * Represents a repository that manages a local list of users.
 *
 * @param initialUsers The [User] items the repository starts with. Their identifiers must be
 *   unique.
 */
class UsersRepositoryLocal(initialUsers: List<User> = emptyList()) : UsersRepository {
  private val users: MutableList<User> = initialUsers.toMutableList()

  init {
    require(users.map { it.uid }.distinct().size == users.size) {
      "UsersRepositoryLocal: initialUsers contains two Users with the same uid"
    }
  }

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllUsers(): List<User> {
    return users.toList()
  }

  override suspend fun getUser(uId: String): User {
    return users.find { it.uid == uId }
        ?: throw NoSuchElementException("UsersRepositoryLocal: User $uId not found")
  }

  override suspend fun getListUsers(uIds: List<String>): List<User> {
    return uIds.map { getUser(it) }
  }

  override suspend fun addUser(user: User) {
    require(users.none { it.uid == user.uid }) {
      "UsersRepositoryLocal: a User with uid ${user.uid} already exists"
    }
    users.add(user)
  }

  override suspend fun editUser(uId: String, newValue: User) {
    require(newValue.uid == uId) {
      "UsersRepositoryLocal: newValue.uid (${newValue.uid}) must be equal to uId ($uId)"
    }

    val index = users.indexOfFirst { it.uid == uId }

    if (index != -1) {
      users[index] = newValue
    } else {
      throw NoSuchElementException("UsersRepositoryLocal: User $uId not found")
    }
  }

  override suspend fun deleteUser(uId: String) {
    val index = users.indexOfFirst { it.uid == uId }
    if (index != -1) {
      users.removeAt(index)
    } else {
      throw NoSuchElementException("UsersRepositoryLocal: User $uId not found")
    }
  }
}
