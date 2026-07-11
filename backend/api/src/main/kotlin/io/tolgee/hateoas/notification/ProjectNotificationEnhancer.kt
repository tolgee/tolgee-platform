package io.tolgee.hateoas.notification

import io.tolgee.hateoas.project.SimpleProjectModelAssembler
import io.tolgee.model.notifications.Notification
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Component

@Component
class ProjectNotificationEnhancer(
  private val projectService: ProjectService,
  private val simpleProjectModelAssembler: SimpleProjectModelAssembler,
) : NotificationEnhancer {
  override fun enhanceNotifications(notifications: Map<Notification, NotificationModel>) {
    val projectIds = notifications.mapNotNull { (source, _) -> source.project?.id }.distinct()
    val projects = projectService.findAll(projectIds).associateBy { it.id }

    notifications.forEach { (source, target) ->
      target.project =
        source.project
          ?.id
          .let { projects[it] }
          ?.let { simpleProjectModelAssembler.toModel(it) }
    }
  }
}
