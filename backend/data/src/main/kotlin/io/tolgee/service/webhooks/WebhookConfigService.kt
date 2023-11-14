package io.tolgee.service.webhooks

import io.tolgee.component.automations.processors.WebhookEventType
import io.tolgee.component.automations.processors.WebhookException
import io.tolgee.component.automations.processors.WebhookExecutor
import io.tolgee.component.automations.processors.WebhookRequest
import io.tolgee.dtos.request.WebhookConfigRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.repository.WebhookConfigRepository
import io.tolgee.service.automations.AutomationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class WebhookConfigService(
  private val webhookConfigRepository: WebhookConfigRepository,
  private val webhookExecutor: WebhookExecutor,
  private val automationService: AutomationService,
) {
  fun get(projectId: Long, id: Long): WebhookConfig {
    return webhookConfigRepository.findByIdAndProjectId(id, projectId)
      ?: throw NotFoundException()
  }

  fun findAllInProject(projectId: Long, pageable: Pageable): Page<WebhookConfig> {
    return webhookConfigRepository.findByProjectId(projectId, pageable)
  }

  fun create(project: Project, dto: WebhookConfigRequest): WebhookConfig {
    val webhookConfig = WebhookConfig(project)
    webhookConfig.url = dto.url
    webhookConfig.webhookSecret = generateRandomWebhookSecret()
    return webhookConfigRepository.save(webhookConfig)
  }

  fun test(projectId: Long, webhookConfigId: Long): Boolean {
    val webhookConfig = get(projectId, webhookConfigId)
    try {
      webhookExecutor.signAndExecute(
        config = webhookConfig,
        data = WebhookRequest(webhookConfigId, WebhookEventType.TEST, null)
      )
    } catch (e: WebhookException) {
      return false
    }
    return true
  }

  @Transactional
  fun update(projectId: Long, id: Long, dto: WebhookConfigRequest): WebhookConfig {
    val webhookConfig = get(projectId, id)
    webhookConfig.url = dto.url
    automationService.updateForWebhookConfig(webhookConfig)
    return webhookConfigRepository.save(webhookConfig)
  }

  @Transactional
  fun delete(projectId: Long, id: Long) {
    val webhookConfig = get(projectId, id)
    automationService.deleteForWebhookConfig(webhookConfig)
    webhookConfigRepository.delete(webhookConfig)
  }

  private fun generateRandomWebhookSecret(): String {
    val bytes = ByteArray(32)
    java.security.SecureRandom().nextBytes(bytes)
    return "whsec_" + Base64.getEncoder().encodeToString(bytes)
  }

  fun find(id: Long): WebhookConfig? {
    return webhookConfigRepository.findById(id).orElse(null)
  }
}
