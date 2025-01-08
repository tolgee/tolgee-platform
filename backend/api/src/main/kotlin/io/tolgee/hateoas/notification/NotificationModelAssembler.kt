package io.tolgee.hateoas.notification

import io.tolgee.api.v2.controllers.NotificationController
import io.tolgee.model.Notification
import org.springframework.data.domain.Page
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport

class NotificationModelAssembler(
    private val enhancers: List<NotificationEnhancer>,
    private val notifications: Page<Notification>,
) : RepresentationModelAssemblerSupport<Notification, NotificationModel>(
    NotificationController::class.java,
    NotificationModel::class.java,
  ) {
    private val toReturn = run {
        val notificationsWithModel = notifications.content.map { notification ->
            notification to NotificationModel(
                id = notification.id,
                projectId = notification.project?.id,
            )
        }
        enhancers.forEach { enhancer ->
            enhancer.enhanceNotifications(notificationsWithModel)
        }
        notificationsWithModel.toMap()
    }

    override fun toModel(view: Notification): NotificationModel {
        return toReturn[view] ?: throw IllegalStateException("Notification $view was not found")
    }
}
