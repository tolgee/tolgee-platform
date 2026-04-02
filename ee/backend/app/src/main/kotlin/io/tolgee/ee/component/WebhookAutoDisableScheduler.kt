package io.tolgee.ee.component

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.service.WebhookConfigService
import io.tolgee.email.EmailService
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.util.Logging
import io.tolgee.util.addDays
import io.tolgee.util.addMinutes
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.util.HtmlUtils
import java.time.Duration
import java.util.Locale

@Component
class WebhookAutoDisableScheduler(
  private val webhookConfigService: WebhookConfigService,
  private val currentDateProvider: CurrentDateProvider,
  private val lockingProvider: LockingProvider,
  private val organizationRoleRepository: OrganizationRoleRepository,
  private val emailService: EmailService,
  private val tolgeeProperties: TolgeeProperties,
  private val schedulingManager: SchedulingManager,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  @EventListener(ApplicationReadyEvent::class)
  fun schedule() {
    val props = tolgeeProperties.webhook
    if (!props.autoDisableEnabled) {
      logger.info("Webhook auto-disable is disabled, skipping scheduling")
      return
    }
    val period = Duration.ofMillis(props.autoDisableCheckPeriodMs)
    schedulingManager.scheduleWithFixedDelay(::checkAndDisable, period)
    logger.debug("Scheduled webhook auto-disable task with period: {}", period)
  }

  fun checkAndDisable() {
    val props = tolgeeProperties.webhook
    val leaseTime = Duration.ofMillis(props.autoDisableLockLeaseTimeMs)
    lockingProvider.withLockingIfFree(LOCK_NAME, leaseTime) {
      runSentryCatching {
        warnFailingWebhooks()
        disableFailingWebhooks()
      }
    }
  }

  private fun warnFailingWebhooks() {
    val props = tolgeeProperties.webhook
    val warningCutoff = currentDateProvider.date.addMinutes(-props.autoDisableWarningAfterHours * 60)

    executeInNewTransaction(transactionManager) {
      val webhooks = webhookConfigService.findWebhooksToWarn(warningCutoff)

      webhooks.forEach { webhook ->
        webhookConfigService.markWarningNotified(webhook)
        sendEmails(webhook, "webhook-failing-warning")
        logger.info(
          "Sent warning email for webhook {} (url: {}) in project {} after {} hours of failures",
          webhook.id,
          webhook.url,
          webhook.project.id,
          props.autoDisableWarningAfterHours,
        )
      }
    }
  }

  private fun disableFailingWebhooks() {
    val props = tolgeeProperties.webhook
    val cutoffDate = currentDateProvider.date.addDays(-props.autoDisableAfterDays)

    executeInNewTransaction(transactionManager) {
      val webhooks = webhookConfigService.findWebhooksToDisable(cutoffDate)

      webhooks.forEach { webhook ->
        webhookConfigService.disableWebhook(webhook)
        sendEmails(webhook, "webhook-disabled")
        logger.info(
          "Auto-disabled webhook {} (url: {}) in project {} after {} days of failures",
          webhook.id,
          webhook.url,
          webhook.project.id,
          props.autoDisableAfterDays,
        )
      }

      if (webhooks.isNotEmpty()) {
        logger.info("Auto-disabled {} failing webhooks", webhooks.size)
      }
    }
  }

  private fun sendEmails(
    webhook: WebhookConfig,
    template: String,
  ) {
    val organization = webhook.project.organizationOwner
    val owners = organizationRoleRepository.getOwners(organization)

    owners.forEach { owner ->
      runSentryCatching {
        emailService.sendEmailTemplate(
          recipient = owner.username,
          template = template,
          locale = Locale.ENGLISH,
          properties =
            mapOf(
              "recipientName" to owner.name,
              "webhookUrl" to HtmlUtils.htmlEscape(webhook.url),
              "projectName" to HtmlUtils.htmlEscape(webhook.project.name),
              "organizationName" to HtmlUtils.htmlEscape(organization.name),
              "autoDisableAfterDays" to tolgeeProperties.webhook.autoDisableAfterDays.toString(),
            ),
        )
      }
    }
  }

  companion object {
    private const val LOCK_NAME = "webhook_auto_disable_lock"
  }
}
