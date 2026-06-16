package io.tolgee.component.automations.processors

import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.model.webhook.WebhookConfig
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class WebhookDeliveryManager(
  private val webhookExecutor: WebhookExecutor,
  private val currentDateProvider: CurrentDateProvider,
  private val webhookAutoDisableChecker: WebhookAutoDisableChecker,
  private val entityManager: EntityManager,
) {
  /**
   * Delivers a single webhook request and applies the shared failure handling:
   * success/failure streak tracking, auto-disable, and requeue-on-failure.
   * Throws [RequeueWithDelayException] when the batch job should retry.
   */
  fun signExecuteAndHandle(
    config: WebhookConfig,
    data: WebhookRequest,
  ) {
    if (!config.enabled) return

    try {
      webhookExecutor.signAndExecute(config, data)
      updateEntity(webhookConfig = config, failing = false)
    } catch (e: Exception) {
      updateEntity(config, true)
      if (webhookAutoDisableChecker.checkAfterFailure(config)) return
      when (e) {
        is WebhookRespondedWithNon200Status -> throw RequeueWithDelayException(
          Message.WEBHOOK_RESPONDED_WITH_NON_200_STATUS,
          cause = e,
          delayInMs = 5000,
        )

        else -> throw RequeueWithDelayException(
          Message.UNEXPECTED_ERROR_WHILE_EXECUTING_WEBHOOK,
          cause = e,
          delayInMs = 5000,
        )
      }
    }
  }

  private fun updateEntity(
    webhookConfig: WebhookConfig,
    failing: Boolean,
  ) {
    webhookConfig.lastExecuted = currentDateProvider.date
    if (!failing) {
      webhookConfig.firstFailed = null
      webhookConfig.autoDisableNotified = false
      entityManager.persist(webhookConfig)
      return
    }
    if (webhookConfig.firstFailed == null) {
      webhookConfig.firstFailed = currentDateProvider.date
    }
    entityManager.persist(webhookConfig)
  }
}
