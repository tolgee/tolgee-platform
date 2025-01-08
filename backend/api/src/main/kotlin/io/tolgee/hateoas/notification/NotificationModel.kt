package io.tolgee.hateoas.notification

import io.tolgee.hateoas.task.TaskModel
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

data class NotificationModel(
  val id: Long,
  val projectId: Long?,
  var linkedTask: TaskModel? = null,
) : RepresentationModel<NotificationModel>(), Serializable
