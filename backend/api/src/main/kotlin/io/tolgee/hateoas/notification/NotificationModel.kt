package io.tolgee.hateoas.notification

import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.hateoas.task.TaskModel
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationModel(
  val id: Long,
  var project: SimpleProjectModel? = null,
  var linkedTask: TaskModel? = null,
) : RepresentationModel<NotificationModel>(), Serializable
