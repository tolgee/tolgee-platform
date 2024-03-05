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
  private val slackConfigRepository: SlackConfigRepository
) {
  fun create(savedSlackMessage: SavedSlackMessage): SavedSlackMessage {
    savedSlackMessage.slackConfig.apply {
      this.savedSlackMessage.add(savedSlackMessage)
      slackConfigRepository.save(this)
    }

    return savedSlackMessageRepository.save(savedSlackMessage)
  }

  fun find(id: Long): SavedSlackMessage? {
    return savedSlackMessageRepository.findById(id).orElse(null)
  }

  fun find(keyId: Long, langTags: Set<String>): List<SavedSlackMessage> {
    val savedSlackMessages = findByKey(keyId)

    return savedSlackMessages.filter { savedSlackMessage ->
      savedSlackMessage.langTags.any { it in langTags }
    }
  }

  fun findByKey(keyId: Long): List<SavedSlackMessage> {
    return savedSlackMessageRepository.findByKeyId(keyId)
  }

  fun getAllSavedSlackMessages(): List<SavedSlackMessage> {
    return savedSlackMessageRepository.findAll()
  }

  fun deleteSavedSlackMessage(id: Long) {
    savedSlackMessageRepository.deleteById(id)
  }

  @Scheduled(cron = "0 0 * * * *")
  fun deleteOldMessage() {
    val cutoff = Date.from(LocalDateTime.now().minusHours(2).atZone(ZoneId.systemDefault()).toInstant())
    savedSlackMessageRepository.deleteOlderThan(cutoff)
  }
}
