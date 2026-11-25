// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.role.Permission
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.task.Task
import com.android.festivar.model.user.User
import java.time.ZonedDateTime

/** The maximum number of [Announcement] items an [Event] can have. */
const val MAX_ANNOUNCEMENTS_PER_EVENT = 100

/**
 * The [Role] given to a user who joins an [Event]. Everyone who joins an [Event] becomes one of its
 * members, simple visitors included: a visitor is a member with this [Role].
 */
val DEFAULT_ROLE = Role.VISITOR

/**
 * Represents an [Event] in the FestivAR application. An [Event] is the core element of the
 * application as it is its purpose. An [Event] can be any organized meeting necessitating people
 * organization as well as a clear overview of it, it can be a festival, a music concert, a wedding
 * etc.
 *
 * An [Event] is immutable: its fields can never be changed. Every "modification" ([copy],
 * [addMember], [removeMember], [addAnnouncement], [close],...) returns a new [Event] instead, so
 * the result must be kept as: `event = event.close()`. And for any other change of one of the
 * [Event] variables, it has to be done through a copy of this [Event]. This implementation is the
 * consequence of working with Compose that needs, to refresh the View automatically, to have an
 * object that is reassigned. Only changing a field of an object would not automatically refresh the
 * View.
 *
 * Every new version goes through the `init` block, so the rules below always hold:
 * - [title] is not blank;
 * - [eventId] is not blank;
 * - [chatIds] contains no blank or duplicate chat identifiers;
 * - [ownerId] identifies one of the members;
 * - a [User] is a joined member at most once, identified by its [User.uid] (no duplicates);
 * - every member has exactly one [Role] in [roles], and only members have one;
 * - the owner has the [Role.OWNER], and nobody else has it;
 * - a [Task] is unique in the event (no duplicates);
 * - an [Announcement] is unique in the event, identified by its [Announcement.announcementId] (no
 *   duplicates);
 * - there are at most [MAX_ANNOUNCEMENTS_PER_EVENT] [announcements];
 * - [endDate] must be after [startDate];
 *
 * @property eventId Unique identifier of the [Event].
 * @property chatIds Unique identifiers of the chats belonging to the [Event].
 * @property ownerId Unique identifier of the user who created and owns the [Event].
 * @property title Name of the [Event], shown as the screen title.
 * @property description Free-text description of specifications about the [Event].
 * @property members People who joined or were assigned the [Event].
 * @property startDate Starting date and time of the [Event].
 * @property endDate Ending date and time of the [Event].
 * @property location Where the [Event] takes place.
 * @property tasks Tasks that are related, that belong to the [Event].
 * @property closed Whether the [Event] is still ongoing or not.
 * @property announcements Announcements shared with the members of the [Event].
 * @property roles The [Role] of each member of the [Event], by their [User.uid]. By default, the
 *   owner has the [Role.OWNER] and every other member of [members] has the [DEFAULT_ROLE].
 */
data class Event(
    val eventId: String,
    val ownerId: String,
    val title: String,
    val description: String = "",
    val members: List<User> = listOf(User(ownerId)),
    val roles: Map<String, Role> = members.associate {
      it.uid to if (it.uid == ownerId) Role.OWNER else DEFAULT_ROLE
    },
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val location: String = "",
    val tasks: List<Task> = emptyList(),
    val closed: Boolean = false,
    val announcements: List<Announcement> = emptyList(),
    val chatIds: List<String> = listOf(eventId),
    val imageUrl: String = "",
) {
  init {
    require(eventId.isNotBlank()) { "The eventId cannot be empty." }
    require(chatIds.none { it.isBlank() }) { "A chatId cannot be empty." }
    require(chatIds.distinct().size == chatIds.size) { "A chatId is duplicated." }
    require(ownerId.isNotBlank()) { "The ownerId cannot be empty." }
    require(title.isNotBlank()) { "The title cannot be empty." }
    require(tasks.map { it.taskId }.distinct().size == tasks.size) { "A task is duplicated." }
    require(members.distinctBy { it.uid }.size == members.size) { "A user is registered twice." }
    require(members.any { it.uid == ownerId }) { "The owner must be a member." }
    require(roles.keys == members.map { it.uid }.toSet()) {
      "Every member must have exactly one role, and only members can have one."
    }
    require(roles[ownerId] == Role.OWNER) { "The owner must have the OWNER role." }
    require(roles.values.count { it == Role.OWNER } == 1) {
      "Only the owner can have the OWNER role."
    }
    require(announcements.distinctBy { it.announcementId }.size == announcements.size) {
      "An announcement is duplicated."
    }
    require(announcements.size <= MAX_ANNOUNCEMENTS_PER_EVENT) {
      "An event cannot have more than $MAX_ANNOUNCEMENTS_PER_EVENT announcements."
    }
    require(endDate.isAfter(startDate)) { "endDate must be strictly after startDate." }
  }

  /**
   * Meant to inform that the [Event] is finished/closed by returning a closed version copy of
   * `this`.
   *
   * @return The [Event] but finished/closed.
   */
  fun close(): Event {
    return copy(closed = true)
  }

  /**
   * Tells whether the [Event] is past at a given time: once it is [closed], or once its [endDate]
   * is reached. An [Event] ending exactly at [at] is past.
   *
   * @param at The time at which to check, usually the current time.
   * @return `true` if `this` is closed or ended at [at], `false` otherwise.
   */
  fun isPast(at: ZonedDateTime): Boolean {
    return closed || !endDate.isAfter(at)
  }

  /**
   * Adds [chatId] to this event's chat identifiers.
   *
   * @return A copy of this event containing [chatId].
   * @throws IllegalArgumentException If [chatId] is blank or already linked to this event.
   */
  fun addChatId(chatId: String): Event {
    require(chatId.isNotBlank()) { "A chatId cannot be empty." }
    require(chatIds.none { it == chatId }) { "A chatId is duplicated." }
    return copy(chatIds = chatIds + chatId)
  }

  /**
   * Removes [chatId] from this event's chat identifiers.
   *
   * @return A copy of this event without [chatId].
   * @throws IllegalArgumentException If [chatId] is not linked to this event.
   */
  fun removeChatId(chatId: String): Event {
    require(chatIds.contains(chatId)) { "This chatId is not linked to the event." }
    return copy(chatIds = chatIds.filterNot { it == chatId })
  }

  /**
   * Adds a new [User] to the [Event] by returning a version of `this` with [user] added to the
   * [members] list, with the [DEFAULT_ROLE] in [roles].
   *
   * @param user The [User] that is going to join `this`, to be added to [members] list.
   * @return The [Event] but with [user] added to it.
   * @throws IllegalArgumentException If [user] is already a member, identified by its [User.uid].
   */
  fun addMember(user: User): Event {
    require(members.none { it.uid == user.uid }) { "This user is already registered in the event." }
    return copy(members = members + user, roles = roles + (user.uid to DEFAULT_ROLE))
  }

  /**
   * Removes a member [User] from the [Event] by returning a version of `this` with the user removed
   * from the [members] list and their [Role] removed from [roles]. The member is identified by the
   * [userId].
   *
   * @param userId The identifier of the [User] that is going to be removed from `this`, from
   *   [members] list.
   * @return The [Event] but with the user removed from it.
   * @throws IllegalArgumentException If the user is not a member, identified by [userId].
   */
  fun removeMember(userId: String): Event {
    require(members.any { it.uid == userId }) { "This user is not a member of the event." }
    require(userId != ownerId) { "The event owner cannot be removed." }

    return copy(members = members.filterNot { it.uid == userId }, roles = roles - userId)
  }

  /**
   * Adds a new [Task] to the [Event] by returning a version of `this` with [task] added to the
   * [tasks] list.
   *
   * @param task The [Task] that is going to be added to `this`, to be added to [tasks] list.
   * @return The [Event] but with [task] added to it.
   * @throws IllegalArgumentException If [task] is already in the event or if the task is not linked
   *   to that event through its [Task.eventId].
   */
  fun addTask(task: Task): Event {
    require(tasks.none { it.taskId == task.taskId }) { "This task is already in the event." }
    require(task.eventId == eventId) { "This task is linked to a different event." }
    return copy(tasks = tasks + task)
  }

  /**
   * Removes a [Task] from the [Event] by returning a version of `this` with [task] removed from the
   * [tasks] list.
   *
   * @param taskId The [Task]'s identifier that is going to be removed from `this`, from [tasks]
   *   list.
   * @return The [Event] but with [taskId]'s [Task] removed from it.
   * @throws IllegalArgumentException If [taskId]'s [Task] is not assigned to `this`.
   */
  fun removeTask(taskId: String): Event {
    require(tasks.any { it.taskId == taskId }) { "This task is not in the event." }
    return copy(tasks = tasks.filterNot { it.taskId == taskId })
  }

  /**
   * Adds a new [Announcement] to the [Event] by returning a version of `this` with [announcement]
   * added to the [announcements] list.
   *
   * @param announcement The [Announcement] that is going to be added to `this`, to be added to
   *   [announcements] list.
   * @return The [Event] but with [announcement] added to it.
   * @throws IllegalArgumentException If an [Announcement] with the same
   *   [Announcement.announcementId] is already in the event, or if the event already has
   *   [MAX_ANNOUNCEMENTS_PER_EVENT] announcements.
   */
  fun addAnnouncement(announcement: Announcement): Event {
    return copy(announcements = announcements.withAnnouncement(announcement))
  }

  /**
   * Edits an [Announcement] of the [Event] by returning a version of `this` where the
   * [Announcement] with the same [Announcement.announcementId] as [announcement] is replaced by it,
   * at the same position in the [announcements] list.
   *
   * @param announcement The new version of the [Announcement], identified by its
   *   [Announcement.announcementId].
   * @return The [Event] but with [announcement] replacing its previous version.
   * @throws IllegalArgumentException If no [Announcement] of the event has the same
   *   [Announcement.announcementId] as [announcement].
   */
  fun editAnnouncement(announcement: Announcement): Event {
    return copy(announcements = announcements.withEditedAnnouncement(announcement))
  }

  /**
   * Removes an [Announcement] from the [Event] by returning a version of `this` with the
   * [Announcement] of [announcementId] removed from the [announcements] list.
   *
   * @param announcementId The identifier of the [Announcement] that is going to be removed from
   *   `this`, from [announcements] list.
   * @return The [Event] but with [announcementId]'s [Announcement] removed from it.
   * @throws IllegalArgumentException If [announcementId]'s [Announcement] is not in the event.
   */
  fun removeAnnouncement(announcementId: String): Event {
    return copy(announcements = announcements.withoutAnnouncement(announcementId))
  }

  /**
   * Finds an [Announcement] of the [Event] by its [Announcement.announcementId].
   *
   * @param announcementId The identifier of the [Announcement] to find in the [announcements] list.
   * @return The [Announcement] of [announcementId], or `null` when the event has no announcement
   *   with this id.
   */
  fun announcementOrNull(announcementId: String): Announcement? {
    return announcements.announcementOrNull(announcementId)
  }

  /**
   * Finds the [Role] of a member of the [Event]. The keys of [roles] are exactly the members, so a
   * user without an entry in [roles] is not a member.
   *
   * @param userId The [User.uid] of the member whose [Role] is searched.
   * @return The [Role] of the member, as stored in [roles].
   * @throws IllegalArgumentException If [userId] is not a member of the event.
   */
  fun roleOf(userId: String): Role {
    return requireNotNull(roles[userId]) { "This user is not a member of the event." }
  }

  /**
   * Tells whether a member of the [Event] is allowed to do an action, that is whether their [Role]
   * has [permission].
   *
   * @param userId The [User.uid] of the member who wants to act.
   * @param permission The [Permission] needed for the action.
   * @return `true` if the [Role] of the member has [permission], `false` otherwise.
   * @throws IllegalArgumentException If [userId] is not a member of the event.
   */
  fun canAct(userId: String, permission: Permission): Boolean {
    return roleOf(userId).hasPermission(permission)
  }

  /**
   * Changes the [Role] of a member of the [Event] by returning a version of `this` where the member
   * [userId] has [newRole] in [roles].
   *
   * The member [actorId] who makes the change must be allowed to, as decided by [canSetRole]: their
   * [Role] must have [Permission.CAN_MANAGE_ROLES] and strictly dominate both the current [Role] of
   * [userId] and [newRole]. So nobody can change their own [Role], change the [Role] of the owner,
   * or give the [Role.OWNER].
   *
   * @param actorId The [User.uid] of the member who changes the [Role].
   * @param userId The [User.uid] of the member whose [Role] is changed.
   * @param newRole The [Role] that [userId] gets.
   * @return The [Event] but with [newRole] as the [Role] of [userId].
   * @throws IllegalArgumentException If [actorId] or [userId] is not a member of the event, or if
   *   [actorId] is not allowed to make this change.
   */
  fun setRole(actorId: String, userId: String, newRole: Role): Event {
    require(canSetRole(actorId, userId, newRole)) {
      "This member cannot give this role to this user."
    }
    return copy(roles = roles + (userId to newRole))
  }

  /**
   * Kicks a member out of the [Event] by returning a version of `this` without them, as
   * [removeMember] does.
   *
   * The member [actorId] who kicks must be allowed to, as decided by [canKick]: their [Role] must
   * have [Permission.CAN_KICK_MEMBER] and strictly dominate the [Role] of [userId]. So nobody can
   * kick themselves or the owner. A member who leaves the event on their own uses [removeMember]
   * instead.
   *
   * @param actorId The [User.uid] of the member who kicks.
   * @param userId The [User.uid] of the member who is kicked.
   * @return The [Event] but without [userId] in [members] and in [roles].
   * @throws IllegalArgumentException If [actorId] or [userId] is not a member of the event, or if
   *   [actorId] is not allowed to kick [userId].
   */
  fun kickMember(actorId: String, userId: String): Event {
    require(canKick(actorId, userId)) { "This member cannot kick this user." }
    return removeMember(userId)
  }

  /**
   * Gives the ownership of the [Event] to another member by returning a version of `this` where
   * [newOwnerId] is the [ownerId] with the [Role.OWNER], and the previous owner [actorId] gets the
   * [Role.ADMIN].
   *
   * Only a member whose [Role] has [Permission.CAN_TRANSFER_OWNERSHIP], that is the owner, can do
   * it.
   *
   * @param actorId The [User.uid] of the owner who gives the ownership.
   * @param newOwnerId The [User.uid] of the member who becomes the owner.
   * @return The [Event] but with [newOwnerId] as its owner and [actorId] as an admin.
   * @throws IllegalArgumentException If [actorId] or [newOwnerId] is not a member of the event, if
   *   [actorId] is not allowed to transfer the ownership, or if [newOwnerId] is [actorId].
   */
  fun transferOwnership(actorId: String, newOwnerId: String): Event {
    require(canAct(actorId, Permission.CAN_TRANSFER_OWNERSHIP)) {
      "This member cannot transfer the ownership of the event."
    }
    require(newOwnerId != actorId) { "This member already owns the event." }
    // Only for its check: the new owner must be a member.
    roleOf(newOwnerId)
    return copy(
        ownerId = newOwnerId,
        roles = roles + (newOwnerId to Role.OWNER) + (actorId to Role.ADMIN),
    )
  }
}

/** [Event.announcementOrNull] on the list alone, also used by EventRepositoryFirebase. */
internal fun List<Announcement>.announcementOrNull(announcementId: String): Announcement? {
  return firstOrNull { it.announcementId == announcementId }
}

/** [Event.addAnnouncement] on the list alone, also used by EventRepositoryFirebase. */
internal fun List<Announcement>.withAnnouncement(announcement: Announcement): List<Announcement> {
  require(none { it.announcementId == announcement.announcementId }) {
    "This announcement is already in the event."
  }
  require(size < MAX_ANNOUNCEMENTS_PER_EVENT) {
    "An event cannot have more than $MAX_ANNOUNCEMENTS_PER_EVENT announcements."
  }
  return this + announcement
}

/** [Event.editAnnouncement] on the list alone, also used by EventRepositoryFirebase. */
internal fun List<Announcement>.withEditedAnnouncement(
    announcement: Announcement
): List<Announcement> {
  require(any { it.announcementId == announcement.announcementId }) {
    "This announcement is not in the event."
  }
  return map { if (it.announcementId == announcement.announcementId) announcement else it }
}

/** [Event.removeAnnouncement] on the list alone, also used by EventRepositoryFirebase. */
internal fun List<Announcement>.withoutAnnouncement(announcementId: String): List<Announcement> {
  require(any { it.announcementId == announcementId }) { "This announcement is not in the event." }
  return filterNot { it.announcementId == announcementId }
}
