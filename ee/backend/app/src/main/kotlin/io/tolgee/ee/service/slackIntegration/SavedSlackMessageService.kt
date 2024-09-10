package io.tolgee.ee.service.slackIntegration

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.component.slackIntegration.SavedMessageDto
import io.tolgee.ee.repository.slackIntegration.SavedSlackMessageRepository
import io.tolgee.ee.repository.slackIntegration.SlackConfigRepository
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.util.addMinutes
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SavedSlackMessageService(
  @Lazy
  private val savedSlackMessageRepository: SavedSlackMessageRepository,
  @Lazy
  private val slackConfigRepository: SlackConfigRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val slackMessageInfoService: SlackMessageInfoService,
) {
  @Transactional
  fun save(
    savedSlackMessage: SavedSlackMessage,
    authorContext: Map<String, String>,
    langTags: Set<String>,
  ): SavedSlackMessage {
    savedSlackMessage.slackConfig.apply {
      this.savedSlackMessage.add(savedSlackMessage)
      slackConfigRepository.save(this)
    }
    addMessageInfo(savedSlackMessage, langTags, authorContext)
    return savedSlackMessageRepository.save(savedSlackMessage)
  }

  private fun addMessageInfo(
    savedSlackMessage: SavedSlackMessage,
    langTags: Set<String>,
    authorContextMap: Map<String, String>,
  ) {
    langTags.forEach {
      val authorContext = authorContextMap[it] ?: ""

      val info =
        slackMessageInfoService.create(
          savedSlackMessage,
          it,
          authorContext,
        )

      savedSlackMessage.info.add(info)
    }
  }

  @Transactional
  fun saveAll(savedSlackMessage: MutableList<SavedSlackMessage>) {
    savedSlackMessageRepository.saveAll(savedSlackMessage)
  }

  @Transactional
  fun update(
    id: Long,
    langTags: Set<String>,
    authorContextMap: Map<String, String>,
  ): SavedSlackMessage? {
    val savedMessage = savedSlackMessageRepository.findById(id).orElse(null) ?: return null
    savedMessage.languageTags = langTags

    langTags.forEach { langTag ->
      val existingInfo = savedMessage.info.find { it.languageTag == langTag }

      if (existingInfo != null) {
        val authorContext = authorContextMap[langTag] ?: return@forEach
        slackMessageInfoService.update(existingInfo, authorContext)
      } else {
        addMessageInfo(savedMessage, setOf(langTag), authorContextMap)
      }
    }

    return savedSlackMessageRepository.save(savedMessage)
  }

  fun find(id: Long): SavedSlackMessage? {
    return savedSlackMessageRepository.findById(id).orElse(null)
  }

  fun find(
    keyId: Long,
    configId: Long,
  ): List<SavedSlackMessage> {
    return savedSlackMessageRepository.findByKeyIdAndSlackConfigId(
      keyId,
      configId,
    )
  }

  fun findAll(
    messagesDto: List<SavedMessageDto>,
    config: Long,
  ): List<SavedSlackMessage> {
    val keyIds = messagesDto.map { it.keyId }

    return savedSlackMessageRepository.findAllByKeyIdAndConfigId(keyIds, config)
  }

  fun findAll(): List<SavedSlackMessage> {
    return savedSlackMessageRepository.findAll()
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional
  fun deleteOldMessage() {
    val cutoff = currentDateProvider.date.addMinutes(-120)
    val oldMessages = savedSlackMessageRepository.findOlderThan(cutoff)

    // Delete related SlackMessageInfo entries
    oldMessages.forEach { message ->
      slackMessageInfoService.deleteAll(message.info)
    }

    savedSlackMessageRepository.deleteAll(oldMessages)
  }
}
