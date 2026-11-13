package io.tolgee.hateoas.notification

import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationEntityKind
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationEntitiesQueryService
import org.springframework.stereotype.Component

@Component
class NotificationEntitiesEnhancer(
  private val queryService: NotificationEntitiesQueryService,
) : NotificationEnhancer {
  override fun enhanceNotifications(notifications: Map<Notification, NotificationModel>) {
    val grouped = notifications.filterKeys { it.type.grouped }
    if (grouped.isEmpty()) return
    val ids = grouped.keys.map { it.id }

    val counts = queryService.getCounts(ids)
    val languages = queryService.getLanguages(ids)
    val branches = queryService.getBranchNames(ids)
    val keySamples = queryService.getKeySamples(ids)

    grouped.forEach { (source, target) ->
      if (source.type.countable) target.entityCount = counts[source.id] ?: 0
      if (source.type.entityKind == NotificationEntityKind.TRANSLATION) {
        target.languages =
          languages[source.id].orEmpty().map { NotificationLanguageModel(it.id, it.tag, it.name, it.flagEmoji) }
      }
      target.branches = branches[source.id].orEmpty()
      if (source.type == NotificationType.KEYS_ADDED) {
        target.sampleKeys =
          keySamples[source.id].orEmpty().map {
            NotificationKeySampleModel(
              it.keyId,
              it.name,
              it.namespace,
              it.baseText,
            )
          }
      }
    }
  }
}
