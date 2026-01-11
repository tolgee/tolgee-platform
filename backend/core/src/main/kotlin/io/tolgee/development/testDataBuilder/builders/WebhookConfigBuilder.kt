package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.webhook.WebhookConfig

class WebhookConfigBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<WebhookConfig, WebhookConfigBuilder> {
  override var self: WebhookConfig = WebhookConfig(projectBuilder.self)
}
