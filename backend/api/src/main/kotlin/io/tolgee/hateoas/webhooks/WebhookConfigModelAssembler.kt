package io.tolgee.hateoas.webhooks

import io.tolgee.api.v2.controllers.webhook.WebhookConfigController
import io.tolgee.model.webhook.WebhookConfig
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class WebhookConfigModelAssembler() : RepresentationModelAssemblerSupport<WebhookConfig, WebhookConfigModel>(
  WebhookConfigController::class.java, WebhookConfigModel::class.java
) {
  override fun toModel(entity: WebhookConfig): WebhookConfigModel {
    return WebhookConfigModel(
      id = entity.id,
      url = entity.url,
      entity.webhookSecret
    )
  }
}
