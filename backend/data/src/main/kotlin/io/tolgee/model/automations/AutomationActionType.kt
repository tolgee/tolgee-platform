package io.tolgee.model.automations

import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.automations.processors.CdnPublishProcessor
import io.tolgee.component.automations.processors.WebhookProcessor
import kotlin.reflect.KClass

enum class AutomationActionType(
  val processor: KClass<out AutomationProcessor>
) {
  CDN_PUBLISH(CdnPublishProcessor::class),
  WEBHOOK(WebhookProcessor::class)
}
