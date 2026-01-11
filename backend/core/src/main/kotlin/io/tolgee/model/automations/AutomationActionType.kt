package io.tolgee.model.automations

import io.tolgee.batch.BatchOperationParams
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.automations.processors.ContentDeliveryPublishProcessor
import io.tolgee.component.automations.processors.SlackSubscriptionProcessor
import io.tolgee.component.automations.processors.WebhookProcessor
import io.tolgee.dtos.cacheable.automations.AutomationActionDto
import io.tolgee.dtos.cacheable.automations.AutomationTriggerDto
import kotlin.reflect.KClass

enum class AutomationActionType(
  val processor: KClass<out AutomationProcessor>,
  val debouncingKeyProvider: ((BatchOperationParams, AutomationActionDto, AutomationTriggerDto) -> Any)? = null,
) {
  CONTENT_DELIVERY_PUBLISH(ContentDeliveryPublishProcessor::class, { params, action, trigger ->
    listOf(params.projectId, action.id, trigger.id)
  }),
  WEBHOOK(WebhookProcessor::class),

  SLACK_SUBSCRIPTION(SlackSubscriptionProcessor::class),
}
