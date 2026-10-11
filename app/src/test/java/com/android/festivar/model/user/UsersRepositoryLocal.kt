// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.user

import java.util.UUID

/**
 * An in-memory [UsersRepository] for unit tests.
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

  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun getAllUsers(): List<User> = users.toList()

  override suspend fun getUser(uId: String): User =
      users.find { it.uid == uId }
          ?: throw NoSuchElementException("UsersRepositoryLocal: User $uId not found")

  override suspend fun getListUsers(uIds: List<String>): List<User> = uIds.map { getUser(it) }

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
    if (index == -1) throw NoSuchElementException("UsersRepositoryLocal: User $uId not found")
    users[index] = newValue
  }

  override suspend fun deleteUser(uId: String) {
    if (!users.removeIf { it.uid == uId }) {
      throw NoSuchElementException("UsersRepositoryLocal: User $uId not found")
    }
  }
}
