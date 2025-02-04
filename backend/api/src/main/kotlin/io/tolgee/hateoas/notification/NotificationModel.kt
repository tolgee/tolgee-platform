package io.tolgee.hateoas.notification

import io.tolgee.constants.NotificationType
import io.tolgee.hateoas.project.SimpleProjectModel
import io.tolgee.hateoas.task.TaskModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable
import java.util.*

data class NotificationModel(
  val id: Long,
  var project: SimpleProjectModel? = null,
  var type: NotificationType,
  var originatingUser: SimpleUserAccountModel? = null,
  var linkedTask: TaskModel? = null,
  var createdAt: Date?,
) : RepresentationModel<NotificationModel>(), Serializable
