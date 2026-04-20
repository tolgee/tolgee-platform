package io.tolgee.component.automations.processors

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.UsingRedisProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.email.EmailService
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.repository.WebhookConfigRepository
import io.tolgee.util.Logging
import io.tolgee.util.addDays
import io.tolgee.util.addMinutes
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Component
class WebhookAutoDisableChecker(
  private val currentDateProvider: CurrentDateProvider,
  private val organizationRoleRepository: OrganizationRoleRepository,
  private val emailService: EmailService,
  private val tolgeeProperties: TolgeeProperties,
  private val webhookConfigRepository: WebhookConfigRepository,
  private val usingRedisProvider: UsingRedisProvider,
  private val applicationContext: ApplicationContext,
) : Logging {
  private val inMemoryTimestamps = ConcurrentHashMap<Long, Long>()

  /**
   * Called after each webhook failure. Checks if the webhook should be warned or disabled.
   * Debounced per webhook ID — runs at most once per 5 minutes.
   *
   * @return true if the webhook was disabled
   */
  fun checkAfterFailure(webhookConfig: WebhookConfig): Boolean {
    if (!tolgeeProperties.webhook.autoDisableEnabled) return false
    if (webhookConfig.firstFailed == null) return false
    if (!shouldCheck(webhookConfig.id)) return false

    val props = tolgeeProperties.webhook
    val now = currentDateProvider.date
    val disableCutoff = now.addDays(-props.autoDisableAfterDays)
    val warningCutoff = now.addMinutes(-props.autoDisableWarningAfterHours * 60)

    if (webhookConfig.firstFailed!! <= disableCutoff) {
      webhookConfig.enabled = false
      webhookConfig.autoDisabled = true
      webhookConfigRepository.save(webhookConfig)
      sendEmails(webhookConfig, "webhook-disabled")
      logger.info(
        "Auto-disabled webhook {} (url: {}) after {} days of failures",
        webhookConfig.id,
        webhookConfig.url,
        props.autoDisableAfterDays,
      )
      return true
    }

    if (!webhookConfig.autoDisableNotified && webhookConfig.firstFailed!! <= warningCutoff) {
      sendEmails(webhookConfig, "webhook-failing-warning")
      webhookConfig.autoDisableNotified = true
      webhookConfigRepository.save(webhookConfig)
      logger.info(
        "Sent warning email for webhook {} (url: {}) after {} hours of failures",
        webhookConfig.id,
        webhookConfig.url,
        props.autoDisableWarningAfterHours,
      )
    }

    return false
  }

  private fun shouldCheck(webhookId: Long): Boolean {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisTemplate = applicationContext.getBean(StringRedisTemplate::class.java)
      val key = "webhook_disable_check:$webhookId"
      return redisTemplate.opsForValue().setIfAbsent(key, "1", DEBOUNCE_DURATION) == true
    }

    val now = System.currentTimeMillis()
    val lastCheck = inMemoryTimestamps[webhookId]
    if (lastCheck != null && now - lastCheck < DEBOUNCE_DURATION.toMillis()) return false
    inMemoryTimestamps[webhookId] = now
    return true
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
    private val DEBOUNCE_DURATION = Duration.ofMinutes(5)
  }
}
