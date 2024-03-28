package io.tolgee.service.slackIntegration

import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.repository.slackIntegration.SavedSlackMessageRepository
import io.tolgee.repository.slackIntegration.SlackConfigRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
class SavedSlackMessageService(
  private val savedSlackMessageRepository: SavedSlackMessageRepository,
  private val slackConfigRepository: SlackConfigRepository,
) {
  fun create(savedSlackMessage: SavedSlackMessage): SavedSlackMessage {
    savedSlackMessage.slackConfig.apply {
      this.savedSlackMessage.add(savedSlackMessage)
      slackConfigRepository.save(this)
    }

    return savedSlackMessageRepository.save(savedSlackMessage)
  }

  fun update(
    id: Long,
    langTags: Set<String>,
  ): SavedSlackMessage? {
    val savedMessage = savedSlackMessageRepository.findById(id).orElse(null) ?: return null
    savedMessage.langTags = langTags

    // Сохраняем изменения
    return savedSlackMessageRepository.save(savedMessage)
  }

  fun find(id: Long): SavedSlackMessage? {
    return savedSlackMessageRepository.findById(id).orElse(null)
  }

  fun find(
    keyId: Long,
    langTags: Set<String>,
    configId: Long,
  ): List<SavedSlackMessage> {
    val savedSlackMessages = findByKey(keyId, configId)

    return savedSlackMessages.filter { savedSlackMessage ->
      savedSlackMessage.langTags.any { it in langTags }
    }
  }

  fun findByKey(
    keyId: Long,
    configId: Long,
  ): List<SavedSlackMessage> {
    return savedSlackMessageRepository.findByKeyIdAndSlackConfigId(keyId, configId)
  }

  fun getAllSavedSlackMessages(): List<SavedSlackMessage> {
    return savedSlackMessageRepository.findAll()
  }

  fun deleteSavedSlackMessage(id: Long) {
    savedSlackMessageRepository.deleteById(id)
  }

  @Scheduled(cron = "*/5 * * * * *")
  fun deleteOldMessage() {
    val cutoff = Date.from(LocalDateTime.now().minusHours(2).atZone(ZoneId.systemDefault()).toInstant())
    savedSlackMessageRepository.deleteOlderThan(cutoff)
  }
}
