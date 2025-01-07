package io.tolgee.ee.service

import io.tolgee.events.OnEntityPreUpdate
import io.tolgee.model.Notification
import io.tolgee.model.UserAccount
import io.tolgee.model.task.Task
import io.tolgee.repository.NotificationRepository
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TaskAssigneeUpdatedNotificationListener(
  private val notificationRepository: NotificationRepository,
) : Logging {
  @EventListener
  fun onTaskAssigneeChange(event: OnEntityPreUpdate) {
    val entity = event.entity

    if (entity !is Task)
      return

    getNewAssignees(event, entity).forEach {
      notificationRepository.save(
        Notification(
          user = it,
          linkedTask = entity,
          originatingUser = entity.author,
          project = entity.project,
        )
      )
    }
  }

  private fun getNewAssignees(event: OnEntityPreUpdate, entity: Task) : List<UserAccount> {
    val assigneesIndex = event.propertyNames?.indexOf("assignees")

    if (assigneesIndex == null || assigneesIndex == -1) {
      logger.warn("'assignees' not found in 'propertyNames': ${event.propertyNames}")
      return emptyList()
    }

    val previousAssignees = event.previousState?.get(assigneesIndex)

    if (previousAssignees !is Iterable<*>) {
      logger.warn("'previousAssignees' is not iterable: $previousAssignees")
      return emptyList()
    }

    return entity.assignees.filterNot { it in previousAssignees }
  }
}
