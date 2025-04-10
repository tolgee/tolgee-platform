package io.tolgee.hateoas.notification

import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.notifications.Notification
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class OriginatingUserNotificationEnhancer(
  private val userAccountService: UserAccountService,
  private val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : NotificationEnhancer {
  override fun enhanceNotifications(notifications: Map<Notification, NotificationModel>) {
    val userIds = notifications.mapNotNull { (source, _) -> source.originatingUser?.id }.distinct()
    val users = userAccountService.findAll(userIds).associateBy { it.id }

    notifications.forEach { (source, target) ->
      target.originatingUser =
        source.originatingUser
          ?.id
          .let {
            users[it]
          }?.let { simpleUserAccountModelAssembler.toModel(it) }
    }
  }
}
