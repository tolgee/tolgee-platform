package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.ee.api.v2.controllers.WebhookConfigController
import io.tolgee.ee.api.v2.hateoas.model.webhooks.WebhookConfigModel
import io.tolgee.model.webhook.WebhookConfig
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class WebhookConfigModelAssembler() : RepresentationModelAssemblerSupport<WebhookConfig, WebhookConfigModel>(
  WebhookConfigController::class.java,
  WebhookConfigModel::class.java,
) {
  override fun toModel(entity: WebhookConfig): WebhookConfigModel {
    return WebhookConfigModel(
      id = entity.id,
      url = entity.url,
      entity.webhookSecret,
      entity.firstFailed?.time,
      entity.lastExecuted?.time,
    )
  }
}
