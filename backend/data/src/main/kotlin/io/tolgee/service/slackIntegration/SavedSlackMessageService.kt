package io.tolgee.service.slackIntegration

import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.repository.slackIntegration.SavedSlackMessageRepository
import io.tolgee.repository.slackIntegration.SlackConfigRepository
import io.tolgee.util.addMinutes
import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SavedSlackMessageService(
  private val savedSlackMessageRepository: SavedSlackMessageRepository,
  private val slackConfigRepository: SlackConfigRepository,
  private val currentDateProvider: CurrentDateProvider,
) {
  @Transactional
  fun create(savedSlackMessage: SavedSlackMessage): SavedSlackMessage {
    savedSlackMessage.slackConfig.apply {
      this.savedSlackMessage.add(savedSlackMessage)
      slackConfigRepository.save(this)
    }

    return savedSlackMessageRepository.save(savedSlackMessage)
  }

  @Transactional
  fun update(
    id: Long,
    langTags: Set<String>,
  ): SavedSlackMessage? {
    val savedMessage = savedSlackMessageRepository.findById(id).orElse(null) ?: return null
    savedMessage.langTags = langTags

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

  @Scheduled(fixedDelay = 60000)
  fun deleteOldMessage() {
    val cutoff = currentDateProvider.date.addMinutes(-120)
    savedSlackMessageRepository.deleteOlderThan(cutoff)
  }
}
