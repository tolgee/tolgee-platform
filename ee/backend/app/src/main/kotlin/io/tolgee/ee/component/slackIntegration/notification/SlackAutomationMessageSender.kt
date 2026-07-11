package io.tolgee.ee.component.slackIntegration.notification

import com.slack.api.model.Attachment
import io.tolgee.ee.component.slackIntegration.SlackChannelMessagesOperations
import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.component.slackIntegration.notification.messageFactory.SlackImportMessageFactory
import io.tolgee.ee.component.slackIntegration.notification.messageFactory.SlackOnKeyAddedMessageFactory
import io.tolgee.ee.component.slackIntegration.notification.messageFactory.SlackTooManyTranslationsMessageFactory
import io.tolgee.ee.component.slackIntegration.notification.messageFactory.SlackTranslationChangeMessageFactory
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.language.LanguageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import kotlin.collections.forEach
import kotlin.collections.plus

@Lazy
@Component
class SlackAutomationMessageSender(
  private val applicationContext: ApplicationContext,
  private val savedSlackMessageService: SavedSlackMessageService,
  private val blocksProvider: SlackNotificationBlocksProvider,
  private val slackOperations: SlackChannelMessagesOperations,
  private val languageService: LanguageService,
  private val slackOnKeyAddedMessageFactory: SlackOnKeyAddedMessageFactory,
  private val slackTranslationChangeMessageFactory: SlackTranslationChangeMessageFactory,
  private val slackImportMessageFactory: SlackImportMessageFactory,
  private val slackTooManyTranslationsMessageFactory: SlackTooManyTranslationsMessageFactory,
) {
  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  companion object {
    const val MAX_MESSAGES_TO_UPDATE = 5
    const val MAX_NEW_MESSAGES_TO_SEND = 5
  }

  fun sendMessageOnTranslationSet(
    slackConfig: SlackConfig,
    data: SlackRequest,
  ) {
    val context = createContext(slackConfig, data)
    if (context.isBigOperation) {
      logger.debug("Too many translations to send message, sending only one message")
      val messageDto = slackTooManyTranslationsMessageFactory.createMessageIfTooManyTranslations(context)
      sendRegularMessageWithSaving(messageDto, context)
      return
    }

    val messagesDto = slackTranslationChangeMessageFactory.createTranslationChangeMessages(context)
    val savedMessagesByKey = savedSlackMessageService.findAll(messagesDto, context.slackConfig.id).groupBy { it.keyId }

    val messagesToUpdate = mutableMapOf<Long, MutableList<SlackMessageDto>>()
    val messagesToCreate = mutableListOf<SlackMessageDto>()

    for (message in messagesDto) {
      if (message.keyId in savedMessagesByKey) {
        messagesToUpdate.getOrPut(message.keyId) { mutableListOf() }.add(message)
        // for explicitly subscribed languages we want to always send the new message on change
        if (isExplicitlySubscribedToAnyUpdatedLanguage(message, context.slackConfig)) {
          messagesToCreate.add(message)
        }
      } else {
        messagesToCreate.add(message)
      }
    }

    for (message in messagesToCreate) {
      logger.debug("Sending message for key ${message.keyId}")
      sendRegularMessageWithSaving(message, context)
    }

    // Execute the message updates only if there are not too many messages to update
    // Otherwise Slack will complain about too many requests
    if (messagesToUpdate.size > MAX_MESSAGES_TO_UPDATE) {
      logger.debug("Too many messages to update, skipping")
    } else {
      logger.debug("Updating {} messages", messagesToUpdate.size)
      for ((keyId, messages) in messagesToUpdate) {
        val messagesToUpdate = savedMessagesByKey[keyId] ?: emptyList()
        messagesToUpdate.forEach { savedMessage ->
          processSavedMessage(savedMessage, messages.first(), context)
        }
      }
    }
  }

  private fun processSavedMessage(
    savedMsg: SavedSlackMessage,
    message: SlackMessageDto,
    context: SlackMessageContext,
  ) {
    val existingLanguages = savedMsg.info.map { it.languageTag }
    val newLanguages = message.languageTags

    val languagesToAdd = existingLanguages - newLanguages

    val additionalAttachments: MutableList<Attachment> = mutableListOf()

    languagesToAdd.forEach { lang ->
      val authorContext = savedMsg.info.find { it.languageTag == lang }?.authorContext

      val attachment = createAttachmentForLanguage(context, lang, message.keyId, authorContext)
      attachment?.let {
        additionalAttachments.add(it)
      }
    }

    val updatedAttachments = additionalAttachments + message.attachments
    val updatedLanguages = message.languageTags + languagesToAdd
    val authorContextMap = savedMsg.info.associate { it.languageTag to it.authorContext }
    val updatedMessageDto =
      message.copy(
        attachments =
          addAuthorContextToAttachments(
            updatedAttachments.toMutableList(),
            authorContextMap,
            context.slackConfig,
          ),
        languageTags = updatedLanguages,
      )

    if (savedMsg.createdKeyBlocks) {
      updatedMessageDto.blocks = emptyList()
    }

    updateMessage(savedMsg, updatedMessageDto, context)
  }

  private fun addAuthorContextToAttachments(
    additionalAttachments: MutableList<Attachment>,
    authorContextMap: Map<String, String>,
    config: SlackConfig,
  ): List<Attachment> {
    authorContextMap.forEach { (langTag, author) ->
      val fullLanguageName = languageService.getByTag(langTag, config.project).name

      additionalAttachments.forEach attachments@{ attachment ->
        if (attachment.blocks[0].toString().contains(fullLanguageName) && attachment.blocks.size != 3) {
          if (author.isEmpty()) {
            return@attachments
          }
          attachment.blocks = attachment.blocks + blocksProvider.getAuthorBlocks(author)
        }
      }
    }

    return additionalAttachments
  }

  /**
   * We send new message only when user is subscribed to the language explicitly
   */
  private fun isExplicitlySubscribedToAnyUpdatedLanguage(
    message: SlackMessageDto,
    config: SlackConfig,
  ): Boolean {
    val languages = message.languageTags
    val subscribedLanguages = config.preferences.mapNotNull { it.languageTag }

    return languages.any { it in subscribedLanguages }
  }

  fun sendMessageOnKeyAdded(
    slackConfig: SlackConfig,
    data: SlackRequest,
  ) {
    val context = createContext(slackConfig, data)
    val messagesDto = slackOnKeyAddedMessageFactory.createKeyAddMessages(context)

    messagesDto.forEach { message ->
      sendRegularMessageWithSaving(message, context)
    }
  }

  private fun updateMessage(
    savedMessage: SavedSlackMessage,
    messageDto: SlackMessageDto,
    context: SlackMessageContext,
  ) {
    val result =
      slackOperations.updateMessage(context.slackToken, context.slackConfig.channelId, savedMessage.messageTimestamp) {
        if (messageDto.blocks.isNotEmpty()) {
          it.blocks(messageDto.blocks)
        }
        it.attachments(sortSlackMessageAttachments(messageDto.attachments.toMutableList()))
      }

    if (result is SlackChannelMessagesOperations.ChatSuccess) {
      updateLangTagsMessage(savedMessage.id, messageDto.languageTags, messageDto.authorContext)
    }
  }

  private fun sendRegularMessageWithSaving(
    messageDto: SlackMessageDto,
    context: SlackMessageContext,
  ) {
    val result =
      slackOperations.sendMessage(context.slackToken, context.slackConfig.channelId, messageDto.blocks) {
        it.attachments(sortSlackMessageAttachments(messageDto.attachments.toMutableList()))
      }
    if (result is SlackChannelMessagesOperations.ChatSuccess) {
      saveMessage(messageDto, result.response.ts, context.slackConfig)
    }
  }

  private fun createContext(
    slackConfig: SlackConfig,
    data: SlackRequest,
  ): SlackMessageContext = SlackMessageContext(applicationContext, slackConfig, data)

  private fun saveMessage(
    messageDto: SlackMessageDto,
    ts: String,
    config: SlackConfig,
  ) {
    savedSlackMessageService.save(
      savedSlackMessage =
        SavedSlackMessage(
          messageTimestamp = ts,
          slackConfig = config,
          keyId = messageDto.keyId,
          languageTags = messageDto.languageTags,
          messageDto.createdKeyBlocks,
        ),
      messageDto.authorContext,
      messageDto.languageTags,
    )
  }

  private fun updateLangTagsMessage(
    id: Long,
    langTags: Set<String>,
    authorContextMap: Map<String, String>,
  ) {
    savedSlackMessageService.update(id, langTags, authorContextMap)
  }

  fun sendMessageOnImport(
    slackConfig: SlackConfig,
    data: SlackRequest,
  ) {
    val context = createContext(slackConfig, data)
    val counts = context.activityData?.counts?.get("Key") ?: return
    if (counts >= 10) {
      val messageDto = slackImportMessageFactory.createImportMessage(context) ?: return
      sendRegularMessageWithSaving(messageDto, context)
    } else {
      val messagesDto = slackOnKeyAddedMessageFactory.createKeyAddMessages(context)
      messagesDto.forEach { message ->
        sendRegularMessageWithSaving(message, context)
      }
    }
  }

  fun createAttachmentForLanguage(
    context: SlackMessageContext,
    langTag: String,
    keyId: Long,
    author: String?,
  ): Attachment? {
    val translation = context.dataProvider.getTranslation(keyId, langTag) ?: return null
    return blocksProvider.createAttachmentForLanguage(context, translation, author)
  }
}
