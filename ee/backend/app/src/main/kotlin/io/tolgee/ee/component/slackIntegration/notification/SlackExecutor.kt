package io.tolgee.ee.component.slackIntegration.notification

import com.slack.api.Slack
import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import io.tolgee.api.IProjectActivityModel
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.slack.SlackUserLoginDto
import io.tolgee.ee.component.slackIntegration.SlackNotConfiguredException
import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.ee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.language.LanguageService
import io.tolgee.util.I18n
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class SlackExecutor(
  private val applicationContext: ApplicationContext,
  private val tolgeeProperties: TolgeeProperties,
  private val savedSlackMessageService: SavedSlackMessageService,
  private val i18n: I18n,
  private val slackUserConnectionService: SlackUserConnectionService,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val slackNotificationBlocksProvider: SlackNotificationBlocksProvider,
  private val slackClient: Slack,
  private val languageService: LanguageService,
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
    request: SlackRequest,
  ) {
    val slackExecutorHelper = getHelper(slackConfig, request)
    val config = slackExecutorHelper.slackConfig
    val count = getModifiedTranslationsCount(slackExecutorHelper)
    val isBigOperation = isBigOperation(slackExecutorHelper, count)
    if (isBigOperation) {
      logger.debug("Too many translations to send message, sending only one message")
      val messageDto = slackExecutorHelper.createMessageIfTooManyTranslations(count)
      sendRegularMessageWithSaving(messageDto, config)
      return
    }

    val messagesDto = slackExecutorHelper.createTranslationChangeMessage()
    val savedMessages = savedSlackMessageService.findAll(messagesDto, config.id)

    val messageUpdateActions: MutableList<() -> Unit> = mutableListOf()

    messagesDto.forEach { message ->
      val messagesToUpdate =
        savedMessages
          .filter { it.keyId == message.keyId }

      messagesToUpdate.forEach { savedMessage ->
        messageUpdateActions.add {
          processSavedMessage(savedMessage, message, config, slackExecutorHelper)
        }
      }

      // Execute the message updates only if there are not too many messages to update
      // Otherwise Slack will complain about too many requests
      if (messageUpdateActions.size > MAX_MESSAGES_TO_UPDATE) {
        logger.debug("Too many messages to update, skipping")
      }

      // We only want to send message when the user is explicitly subscribed to the language.
      // If they're subscribed to all languages, we just update the previous messages to not spam so much
      if (!isExplicitlySubscribedToAnyUpdatedLanguage(message, config) && messagesToUpdate.isNotEmpty()) {
        logger.debug(
          "User is not subscribed to any of the languages and there is message to update with a new value.\n" +
            "Not sending new message.",
        )
        return@forEach
      }

      logger.debug("Sending message for key ${message.keyId}")
      sendRegularMessageWithSaving(message, config)
    }

    executeUpdateActions(messageUpdateActions)
  }

  private fun isBigOperation(
    slackExecutorHelper: SlackExecutorHelper,
    count: Long,
  ): Boolean {
    if (count > MAX_NEW_MESSAGES_TO_SEND) {
      return true
    }

    // This happens in case that the data are considered big in the view provider and so it is not loaded
    // In that case we just also consider it big
    // However, we still need to check whether there are any translations changed
    return slackExecutorHelper.data.activityData?.let { getTranslationChangeSizeFromModifiedEntities(it) } == null &&
      count > 0
  }

  private fun getModifiedTranslationsCount(slackExecutorHelper: SlackExecutorHelper): Long {
    val activityData = slackExecutorHelper.data.activityData ?: return 0

    val countFromCounts = activityData.counts?.get("Translation")

    // for activities with a lot of data, we get count only in the counts map
    if (countFromCounts != null) {
      return countFromCounts
    }

    // for small activities, we get count from modifiedEntities
    return getTranslationChangeSizeFromModifiedEntities(activityData) ?: 0
  }

  /**
   * If this is empty, it means that the operation is probably big
   */
  private fun getTranslationChangeSizeFromModifiedEntities(activityData: IProjectActivityModel) =
    activityData.modifiedEntities?.get("Translation")?.size?.toLong()

  private fun executeUpdateActions(messageUpdateActions: MutableList<() -> Unit>) {
    logger.debug("Updating ${messageUpdateActions.size} messages")
    messageUpdateActions.forEach { it() }
  }

  fun getSlackNickName(authorId: Long): String? {
    val slackId = slackUserConnectionService.findByUserAccountId(authorId)?.slackUserId ?: return null
    return "<@$slackId>"
  }

  private fun processSavedMessage(
    savedMsg: SavedSlackMessage,
    message: SlackMessageDto,
    config: SlackConfig,
    slackExecutorHelper: SlackExecutorHelper,
  ) {
    val existingLanguages = savedMsg.info.map { it.languageTag }
    val newLanguages = message.languageTags

    val languagesToAdd = existingLanguages - newLanguages

    val additionalAttachments: MutableList<Attachment> = mutableListOf()

    languagesToAdd.forEach { lang ->
      val authorContext = savedMsg.info.find { it.languageTag == lang }?.authorContext

      val attachment = slackExecutorHelper.createAttachmentForLanguage(lang, message.keyId, authorContext)
      attachment?.let {
        additionalAttachments.add(it)
      }
    }

    val updatedAttachments = additionalAttachments + message.attachments
    val updatedLanguages = message.languageTags + languagesToAdd
    val authorContextMap = savedMsg.info.associate { it.languageTag to it.authorContext }
    val updatedMessageDto =
      message.copy(
        attachments = addAuthorContextToAttachments(updatedAttachments.toMutableList(), authorContextMap, config),
        languageTags = updatedLanguages,
      )

    if (savedMsg.createdKeyBlocks) {
      updatedMessageDto.blocks = emptyList()
    }

    updateMessage(savedMsg, config, updatedMessageDto)
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
          attachment.blocks = attachment.blocks + slackNotificationBlocksProvider.getAuthorBlocks(author)
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

  fun sortAttachments(attachments: MutableList<Attachment>): MutableList<Attachment> {
    fun getLanguageName(attachment: Attachment): String {
      val textBlock = attachment.blocks[0].toString()
      // Assuming the language name is surrounded by asterisks (e.g., 🇸🇬 *Chinese*)
      return textBlock.substringAfter('*').substringBefore('*').trim()
    }

    val baseLanguageAttachmentIndex =
      attachments.indexOfFirst {
        it.blocks[0].toString().contains("(base)")
      }
    val baseLanguageAttachment =
      if (baseLanguageAttachmentIndex != -1) {
        attachments.removeAt(baseLanguageAttachmentIndex)
      } else {
        null
      }

    val buttonAttachment = attachments.removeAt(attachments.size - 1)

    attachments.sortBy { getLanguageName(it) }

    baseLanguageAttachment?.let {
      attachments.add(0, it)
    }

    attachments.add(buttonAttachment)

    return attachments
  }

  fun sendMessageOnKeyAdded(
    slackConfig: SlackConfig,
    request: SlackRequest,
  ) {
    val slackExecutorHelper = getHelper(slackConfig, request)
    val config = slackExecutorHelper.slackConfig
    val messagesDto = slackExecutorHelper.createKeyAddMessage()

    messagesDto.forEach { message ->
      sendRegularMessageWithSaving(message, config)
    }
  }

  fun sendUserLoginSuccessMessage(
    token: String,
    dto: SlackUserLoginDto,
  ) {
    slackClient.methods(token).chatPostEphemeral {
      it.user(dto.slackUserId)
      it.channel(dto.slackChannelId)
      it.blocks(slackNotificationBlocksProvider.getUserLoginSuccessBlocks())
    }
  }

  fun sendBlocksMessage(
    teamId: String,
    channelId: String,
    blocks: List<LayoutBlock>,
  ) {
    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(teamId)
    slackClient.methods(workspace.getSlackToken()).chatPostMessage {
      it.channel(channelId)
        .blocks(blocks)
    }
  }

  private fun updateMessage(
    savedMessage: SavedSlackMessage,
    config: SlackConfig,
    messageDto: SlackMessageDto,
  ) {
    val response =
      slackClient.methods(config.organizationSlackWorkspace.getSlackToken()).chatUpdate { request ->
        request
          .channel(config.channelId)
          .ts(savedMessage.messageTimestamp)
          .attachments(sortAttachments(messageDto.attachments.toMutableList()))
        if (messageDto.blocks.isNotEmpty()) {
          request.blocks(messageDto.blocks)
        }
        request
      }

    if (response.isOk) {
      updateLangTagsMessage(savedMessage.id, messageDto.languageTags, messageDto.authorContext)
    } else {
      RuntimeException("Cannot update message in slack: ${response.error}")
        .let { logger.error(it.message, it) }
    }
  }

  private fun sendRegularMessageWithSaving(
    messageDto: SlackMessageDto,
    config: SlackConfig,
  ) {
    val response =
      slackClient.methods(config.organizationSlackWorkspace.getSlackToken()).chatPostMessage { request ->
        request.channel(config.channelId)
          .blocks(messageDto.blocks)
          .attachments(sortAttachments(messageDto.attachments.toMutableList()))
      }
    if (response.isOk) {
      saveMessage(messageDto, response.ts, config)
    } else {
      RuntimeException("Cannot send message in slack: ${response.error}")
        .let { logger.error(it.message, it) }
    }
  }

  fun getHelper(
    slackConfig: SlackConfig,
    data: SlackRequest,
  ): SlackExecutorHelper {
    return SlackExecutorHelper(
      applicationContext = applicationContext,
      slackConfig,
      data,
      i18n,
      getSlackNickName(data.activityData?.author?.id ?: 0L),
    )
  }

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

  private fun OrganizationSlackWorkspace?.getSlackToken(): String {
    return this?.accessToken ?: tolgeeProperties.slack.token ?: throw SlackNotConfiguredException()
  }

  fun sendMessageOnImport(
    slackConfig: SlackConfig,
    request: SlackRequest,
  ) {
    val slackExecutorHelper = getHelper(slackConfig, request)
    val config = slackExecutorHelper.slackConfig
    val counts = slackExecutorHelper.data.activityData?.counts?.get("Key") ?: return
    if (counts >= 10) {
      val messageDto = slackExecutorHelper.createImportMessage() ?: return
      sendRegularMessageWithSaving(messageDto, config)
    } else {
      sendMessageOnKeyAdded(slackConfig, request)
    }
  }
}
