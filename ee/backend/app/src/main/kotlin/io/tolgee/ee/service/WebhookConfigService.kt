package io.tolgee.ee.service

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
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class WebhookConfigService(
  private val webhookConfigRepository: WebhookConfigRepository,
  private val webhookExecutor: WebhookExecutor,
  private val automationService: AutomationService,
) {
  fun get(
    projectId: Long,
    id: Long,
  ): WebhookConfig {
    return webhookConfigRepository.findByIdAndProjectId(id, projectId)
      ?: throw NotFoundException()
  }

  fun get(id: Long): WebhookConfig {
    return webhookConfigRepository.findById(id).orElseThrow { NotFoundException() }
  }

  fun findAllInProject(
    projectId: Long,
    pageable: Pageable,
  ): Page<WebhookConfig> {
    return webhookConfigRepository.findByProjectId(projectId, pageable)
  }

  fun create(
    project: Project,
    dto: WebhookConfigRequest,
  ): WebhookConfig {
    val webhookConfig = WebhookConfig(project)
    webhookConfig.url = dto.url
    webhookConfig.webhookSecret = generateRandomWebhookSecret()
    webhookConfigRepository.save(webhookConfig)
    automationService.createForWebhookConfig(webhookConfig)
    return webhookConfig
  }

  fun test(
    projectId: Long,
    webhookConfigId: Long,
  ): Boolean {
    val webhookConfig = get(projectId, webhookConfigId)
    try {
      webhookExecutor.signAndExecute(
        config = webhookConfig,
        data = WebhookRequest(webhookConfigId, WebhookEventType.TEST, null),
      )
    } catch (e: WebhookException) {
      return false
    }
    return true
  }

  @Transactional
  fun update(
    projectId: Long,
    id: Long,
    dto: WebhookConfigRequest,
  ): WebhookConfig {
    val webhookConfig = get(projectId, id)
    webhookConfig.url = dto.url
    automationService.updateForWebhookConfig(webhookConfig)
    return webhookConfigRepository.save(webhookConfig)
  }

  @Transactional
  fun delete(
    projectId: Long,
    id: Long,
  ) {
    val webhookConfig = get(projectId, id)
    automationService.deleteForWebhookConfig(webhookConfig)
    webhookConfigRepository.delete(webhookConfig)
  }

  private fun generateRandomWebhookSecret(): String {
    val hex = (1..32).joinToString("") { (0..15).random().toString(16) }
    return "whsec_$hex"
  }

  fun find(id: Long): WebhookConfig? {
    return webhookConfigRepository.findById(id).orElse(null)
  }
}
