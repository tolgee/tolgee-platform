package io.tolgee.hateoas.notification

import io.tolgee.api.v2.controllers.notification.NotificationController
import io.tolgee.model.notifications.Notification
import org.springframework.data.domain.Page
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport

class NotificationModelAssembler(
  private val enhancers: List<NotificationEnhancer>,
  private val notifications: Page<Notification>,
) : RepresentationModelAssemblerSupport<Notification, NotificationModel>(
    NotificationController::class.java,
    NotificationModel::class.java,
  ) {
  private val prefetchedNotifications =
    run {
      val notificationsWithModel =
        notifications.content.associateWith { notification ->
          NotificationModel(
            id = notification.id,
            type = notification.type,
            createdAt = notification.createdAt,
          )
        }
      enhancers.forEach { enhancer ->
        enhancer.enhanceNotifications(notificationsWithModel)
      }
      notificationsWithModel
    }

  override fun toModel(view: Notification): NotificationModel {
    return prefetchedNotifications[view] ?: throw IllegalStateException("Notification $view was not found")
  }
}
