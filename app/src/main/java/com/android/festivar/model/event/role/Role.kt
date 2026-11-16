package com.android.festivar.model.event.role

import com.android.festivar.model.event.role.Permission.*

enum class Role(val permissions: Set<Permission>) {
  VISITOR(setOf(
    CAN_SEE_ANNOUNCEMENT,
  )),
  STAFF(setOf(
    CAN_SEE_TASK,
    CAN_EDIT_OWN_TASK,
    CAN_DELETE_OWN_TASK,
    CAN_CREATE_TASK,
    CAN_SEE_ANNOUNCEMENT,
  )),
  TEAM_LEAD(setOf(
    CAN_SEE_TASK,
    CAN_EDIT_OWN_TASK,
    CAN_DELETE_OWN_TASK,
    CAN_EDIT_TASK,
    CAN_DELETE_TASK,
    CAN_CREATE_TASK,
    CAN_SEE_ANNOUNCEMENT,
    CAN_EDIT_OWN_ANNOUNCEMENT,
    CAN_DELETE_OWN_ANNOUNCEMENT,
    CAN_CREATE_ANNOUNCEMENT,
  )),
  STAFF_MANAGER(setOf(
    CAN_SEE_TASK,
    CAN_EDIT_OWN_TASK,
    CAN_DELETE_OWN_TASK,
    CAN_EDIT_TASK,
    CAN_DELETE_TASK,
    CAN_CREATE_TASK,
    CAN_SEE_ANNOUNCEMENT,
    CAN_EDIT_OWN_ANNOUNCEMENT,
    CAN_DELETE_OWN_ANNOUNCEMENT,
    CAN_EDIT_ANNOUNCEMENT,
    CAN_DELETE_ANNOUNCEMENT,
    CAN_CREATE_ANNOUNCEMENT,
    CAN_EDIT_EVENT,
    CAN_MANAGE_ROLES,
  )),
  ADMIN(setOf(
    CAN_SEE_TASK,
    CAN_EDIT_OWN_TASK,
    CAN_DELETE_OWN_TASK,
    CAN_EDIT_TASK,
    CAN_DELETE_TASK,
    CAN_CREATE_TASK,
    CAN_SEE_ANNOUNCEMENT,
    CAN_EDIT_OWN_ANNOUNCEMENT,
    CAN_DELETE_OWN_ANNOUNCEMENT,
    CAN_EDIT_ANNOUNCEMENT,
    CAN_DELETE_ANNOUNCEMENT,
    CAN_CREATE_ANNOUNCEMENT,
    CAN_EDIT_EVENT,
    CAN_MANAGE_ROLES,
    CAN_KICK_MEMBER,
  )),
  OWNER(
    Permission.entries.toSet()
  );

  fun hasPermission(permission: Permission): Boolean{
    return permissions.contains(permission)
  }

  fun dominates(that: Role): Boolean{
    return permissions.containsAll(that.permissions) && permissions.size > that.permissions.size
  }

  fun comparable(that: Role): Boolean{
    return this.dominates(that) || that.dominates(this)
  }

  fun canManageTo(from: Role, to: Role): Boolean{
    return permissions.contains(CAN_MANAGE_ROLES) && dominates(from) && dominates(to)
  }
}