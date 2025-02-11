package io.tolgee.ee.component

import io.tolgee.ee.api.v2.hateoas.assemblers.TaskModelAssembler
import io.tolgee.ee.service.TaskService
import io.tolgee.hateoas.notification.NotificationEnhancer
import io.tolgee.hateoas.notification.NotificationModel
import io.tolgee.model.notifications.Notification
import org.springframework.stereotype.Component

@Component
class TaskNotificationEnhancer(
  private val taskService: TaskService,
  private val taskModelAssembler: TaskModelAssembler,
) : NotificationEnhancer {
  override fun enhanceNotifications(notifications: Map<Notification, NotificationModel>) {
    val taskIds = notifications.mapNotNull { (source, _) -> source.linkedTask?.id }
    val convertedTasks = taskService.getTasksWithScope(taskIds)

    notifications.forEach { (source, target) ->
      target.linkedTask =
        convertedTasks[source.linkedTask?.id]
          ?.let { taskModelAssembler.toModel(it) }
    }
  }
}
