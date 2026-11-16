package com.android.festivar.model.event.role

/**
 * An enum class that represents all the permissions that can be attributed to a [Role] within an
 * [com.android.festivar.model.event.Event] in the FestivAR app.
 */
enum class Permission {
  CAN_SEE_TASK,
  CAN_EDIT_OWN_TASK,
  CAN_DELETE_OWN_TASK,
  CAN_EDIT_TASK,
  CAN_DELETE_TASK,
  CAN_CREATE_TASK,
  CAN_ASSIGN_TASK,
  CAN_SEE_ANNOUNCEMENT,
  CAN_EDIT_OWN_ANNOUNCEMENT,
  CAN_DELETE_OWN_ANNOUNCEMENT,
  CAN_EDIT_ANNOUNCEMENT,
  CAN_DELETE_ANNOUNCEMENT,
  CAN_CREATE_ANNOUNCEMENT,
  CAN_EDIT_EVENT,
  CAN_DELETE_EVENT,
  CAN_MANAGE_ROLES,
  CAN_KICK_MEMBER,
  CAN_TRANSFER_OWNERSHIP,
}
