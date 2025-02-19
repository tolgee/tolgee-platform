package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.Notification

/**
 * Dynamic component enhancing notifications by additional information if eligible,
 * e.g. adding linked task.
 */
fun interface NotificationEnhancer {
  /**
   * Takes list of input Notification and output NotificationModel.
   * It iterates over the pairs and alters the output NotificationModel by enhancing it of the new information.
   */
  fun enhanceNotifications(notifications: Map<Notification, NotificationModel>)
}
