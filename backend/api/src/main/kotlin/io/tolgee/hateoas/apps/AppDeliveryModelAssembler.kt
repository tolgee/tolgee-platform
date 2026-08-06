package io.tolgee.hateoas.apps

import io.tolgee.model.apps.AppDelivery
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class AppDeliveryModelAssembler : RepresentationModelAssembler<AppDelivery, AppDeliveryModel> {
  override fun toModel(entity: AppDelivery): AppDeliveryModel {
    return AppDeliveryModel(
      id = entity.id,
      eventType = entity.eventType.wireName,
      targetUrl = entity.targetUrl,
      organizationId = entity.organization?.id,
      createdAt = entity.createdAt?.time ?: 0,
      attempts = entity.attempts,
      lastAttemptAt = entity.lastAttemptAt?.time,
      lastError = entity.lastError,
      deliveredAt = entity.deliveredAt?.time,
      abandonedAt = entity.abandonedAt?.time,
    )
  }
}
