package io.tolgee.ee.component.slackIntegration.notification

import com.slack.api.model.Attachment
import io.tolgee.api.IModifiedEntityModel
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.component.slackIntegration.SlackChannelMessagesOperations
import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.component.slackIntegration.data.SlackTranslationInfoDto
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.service.language.LanguageService
import io.tolgee.util.I18n
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.forEach
import kotlin.collections.plus

@Lazy
@Component
class SlackAutomationMessageSender(
  private val applicationContext: ApplicationContext,
  private val tolgeeProperties: TolgeeProperties,
  private val savedSlackMessageService: SavedSlackMessageService,
  private val i18n: I18n,
  private val blocksProvider: SlackNotificationBlocksProvider,
  private val slackOperations: SlackChannelMessagesOperations,
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
    data: SlackRequest,
  ) {
    val context = createContext(slackConfig, data)
    val count = context.modifiedTranslationsCount
    if (context.isBigOperation) {
      logger.debug("Too many translations to send message, sending only one message")
      val messageDto = createMessageIfTooManyTranslations(context, count)
      sendRegularMessageWithSaving(messageDto, context)
      return
    }

    val messagesDto = createTranslationChangeMessage(context)
    val savedMessages = savedSlackMessageService.findAll(messagesDto, context.slackConfig.id)

    val messageUpdateActions: MutableList<() -> Unit> = mutableListOf()

    messagesDto.forEach { message ->
      val messagesToUpdate =
        savedMessages
          .filter { it.keyId == message.keyId }

      messagesToUpdate.forEach { savedMessage ->
        messageUpdateActions.add {
          processSavedMessage(savedMessage, message, context)
        }
      }

      // Execute the message updates only if there are not too many messages to update
      // Otherwise Slack will complain about too many requests
      if (messageUpdateActions.size > MAX_MESSAGES_TO_UPDATE) {
        logger.debug("Too many messages to update, skipping")
      }

      // We only want to send message when the user is explicitly subscribed to the language.
      // If they're subscribed to all languages, we just update the previous messages to not spam so much
      if (!isExplicitlySubscribedToAnyUpdatedLanguage(message, context.slackConfig) && messagesToUpdate.isNotEmpty()) {
        logger.debug(
          "User is not subscribed to any of the languages and there is message to update with a new value.\n" +
            "Not sending new message.",
        )
        return@forEach
      }

      logger.debug("Sending message for key ${message.keyId}")
      sendRegularMessageWithSaving(message, context)
    }

    executeUpdateActions(messageUpdateActions)
  }

  private fun executeUpdateActions(messageUpdateActions: MutableList<() -> Unit>) {
    logger.debug("Updating ${messageUpdateActions.size} messages")
    messageUpdateActions.forEach { it() }
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
    val messagesDto = createKeyAddMessage(context)

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
      val messageDto = createImportMessage(context) ?: return
      sendRegularMessageWithSaving(messageDto, context)
    } else {
      sendMessageOnKeyAdded(slackConfig, data)
    }
  }

  fun createKeyAddMessage(context: SlackMessageContext): List<SlackMessageDto> {
    val activities = context.activityData ?: return emptyList()

    val messages: MutableList<SlackMessageDto> = mutableListOf()
    val modifiedEntities = activities.modifiedEntities ?: return emptyList()
    modifiedEntities.flatMap { (entityType, modifiedEntityList) ->
      modifiedEntityList.map modifiedEntitiesList@{ modifiedEntity ->
        when (entityType) {
          "Key" -> {
            createMessageForAddKey(context, modifiedEntity)?.let { messages.add(it) }
          }

          else -> {
            return@modifiedEntitiesList
          }
        }
      }
    }

    return messages
  }

  private fun createMessageForAddKey(
    context: SlackMessageContext,
    modifiedEntity: IModifiedEntityModel,
  ): SlackMessageDto? {
    val attachments = mutableListOf<Attachment>()
    val langTags: MutableSet<String> = mutableSetOf()

    val keyId = modifiedEntity.entityId
    val key = context.dataProvider.getKeyInfo(keyId)
    val blocksHeader = blocksProvider.getKeyInfoBlock(context, key, i18n.translate("slack.common.message.new-key"))
    val keyTranslations = context.dataProvider.getKeyTranslations(key.id)

    keyTranslations.forEach translations@{ translation ->
      if (!shouldProcessEventNewKeyAdded(context, translation.languageTag, context.baseLanguage.tag)) {
        return@translations
      }

      val attachment = createAttachmentForLanguage(context, translation, null) ?: return@translations

      attachments.add(attachment)
      langTags.add(translation.languageTag)
    }

    if (!langTags.contains(context.baseLanguage.tag) && langTags.isNotEmpty()) {
      keyTranslations.find { it.languageId == context.baseLanguage.id }?.let { baseTranslation ->
        val attachment = createAttachmentForLanguage(context, baseTranslation, null) ?: return@let

        attachments.add(attachment)
        langTags.add(context.baseLanguage.tag)
      }
    }

    attachments.add(blocksProvider.getRedirectButtonAttachment(getUrlOnSpecifiedKey(context, key.id)))

    if (langTags.isEmpty()) {
      return null
    }

    return SlackMessageDto(
      blocks = blocksHeader,
      attachments = attachments,
      keyId = keyId,
      languageTags = langTags,
      true,
    )
  }

  fun createImportMessage(context: SlackMessageContext): SlackMessageDto? {
    val importedCount = context.activityData?.counts?.get("Key") ?: return null

    return SlackMessageDto(
      blocks = blocksProvider.getImportBlocks(context, importedCount),
      attachments = listOf(blocksProvider.getRedirectButtonAttachment(getUrlOnImport(context))),
      0L,
      setOf(),
    )
  }

  fun createTranslationChangeMessage(context: SlackMessageContext): MutableList<SlackMessageDto> {
    val result: MutableList<SlackMessageDto> = mutableListOf()

    val activityData = context.activityData
    activityData?.modifiedEntities?.forEach modifiedEntities@{ (_, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        val event =
          when {
            modifiedEntity.modifications?.contains("text") == true -> "translation"
            modifiedEntity.modifications?.contains("state") == true -> "state"
            else -> ""
          }

        val translationId = modifiedEntity.entityId
        val translation = context.dataProvider.getTranslationById(translationId) ?: return@modifiedEntities
        result.add(
          processTranslationChange(
            context,
            translation,
            getModificationAuthorContext(context, event, activityData.timestamp),
            event,
          ) ?: return@modifiedEntities,
        )

        val baseLanguageTag = context.slackConfig.project.baseLanguage?.tag ?: return@modifiedEntities
        if (baseLanguageTag == translation.languageTag) {
          return@modifiedEntities
        }
      }
    }

    return result
  }

  private fun processTranslationChange(
    context: SlackMessageContext,
    translation: SlackTranslationInfoDto,
    modificationAuthor: String?,
    event: String,
  ): SlackMessageDto? {
    val baseLanguageTag = context.slackConfig.project.baseLanguage?.tag ?: return null
    val modifiedLangTag = translation.languageTag
    val isBaseChanged = modifiedLangTag == baseLanguageTag

    if (!shouldProcessEventTranslationChanged(context, modifiedLangTag, baseLanguageTag, modifiedLangTag)) return null

    val langName =
      if (isBaseChanged) {
        "base language"
      } else {
        translation.languageName
      }

    val key = context.dataProvider.getKeyInfo(translation.keyId)

    val headerBlock =
      blocksProvider.getKeyInfoBlock(
        context,
        key,
        i18n.translate("slack.common.message.new-translation").format(langName, event),
      )
    val attachments =
      mutableListOf(createAttachmentForLanguage(context, translation, modificationAuthor) ?: return null)
    val langTags = mutableSetOf(modifiedLangTag)

    addLanguagesIfNeed(context, attachments, langTags, key.id, modifiedLangTag, baseLanguageTag)
    attachments.add(blocksProvider.getRedirectButtonAttachment(getUrlOnSpecifiedKey(context, key.id)))

    return if (langTags.isEmpty()) {
      null
    } else {
      SlackMessageDto(
        blocks = headerBlock,
        attachments = attachments,
        keyId = key.id,
        languageTags = langTags,
        false,
        isBaseChanged,
        authorContext = mapOf(modifiedLangTag to (modificationAuthor ?: "")),
      )
    }
  }

  private fun getModificationAuthorContext(
    context: SlackMessageContext,
    event: String,
    timestamp: Long,
  ): String {
    val authorMention = context.authorMention
    val correctTimestamp = timestamp / 1000 // Convert milliseconds to seconds

    // fallback to timestamp in case of any error
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = formatter.format(date)

    val formattedTimestamp = "<!date^$correctTimestamp^{time}|$formattedTime>"
    return i18n.translate("slack.common.message.modification-info").format(authorMention, event, formattedTimestamp)
  }

  private fun addLanguagesIfNeed(
    context: SlackMessageContext,
    attachments: MutableList<Attachment>,
    addedTags: MutableSet<String>,
    keyId: Long,
    modifiedLangTag: String,
    baseLanguageTag: String,
  ) {
    context.slackConfig.project.languages.forEach { language ->
      if (!shouldProcessEventTranslationChanged(
          context,
          modifiedLangTag,
          baseLanguageTag,
          language.tag,
        ) && language.tag != baseLanguageTag
      ) {
        return@forEach
      }

      if (shouldAddLanguage(addedTags, modifiedLangTag, language.tag, baseLanguageTag)) {
        createAttachmentForLanguage(context, language.tag, keyId, null)?.let { attachment ->
          attachments.add(attachment)
          addedTags.add(language.tag)
        }
      }
    }
  }

  private fun shouldAddLanguage(
    addedTags: Set<String>,
    modifiedLangTag: String,
    currentLangTag: String,
    baseLanguageTag: String,
  ): Boolean =
    !addedTags.contains(currentLangTag) &&
      (modifiedLangTag == baseLanguageTag || currentLangTag == baseLanguageTag)

  private fun shouldProcessEventTranslationChanged(
    context: SlackMessageContext,
    modifiedLangTag: String,
    baseLanguageTag: String,
    currentLangTag: String,
  ): Boolean {
    val slackConfig = context.slackConfig

    val isBaseChanged = modifiedLangTag == baseLanguageTag
    val globalEventHandlingPreferences =
      if (slackConfig.isGlobalSubscription) {
        Triple(
          slackConfig.events.contains(SlackEventType.ALL),
          slackConfig.events.contains(SlackEventType.BASE_CHANGED) && isBaseChanged,
          slackConfig.events.contains(SlackEventType.TRANSLATION_CHANGED) && !isBaseChanged,
        )
      } else {
        null
      }

    val languageSpecificPreferences =
      slackConfig.preferences.find { it.languageTag == currentLangTag }?.let { pref ->
        Triple(
          pref.events.contains(SlackEventType.ALL),
          pref.events.contains(SlackEventType.BASE_CHANGED) && isBaseChanged,
          pref.events.contains(SlackEventType.TRANSLATION_CHANGED) && !isBaseChanged,
        )
      }

    if (globalEventHandlingPreferences != null) {
      val (isAllEvent, isBaseLanguageChangedEvent, isTranslationChangedEvent) = globalEventHandlingPreferences

      if (isAllEvent || isBaseLanguageChangedEvent || isTranslationChangedEvent) {
        return true
      }
    }

    if (languageSpecificPreferences != null) {
      val (isAllEvent, isBaseLanguageChangedEvent, isTranslationChangedEvent) = languageSpecificPreferences

      if (isAllEvent || isBaseLanguageChangedEvent || isTranslationChangedEvent) {
        return true
      }
    }

    if (isBaseChanged) {
      slackConfig.project.languages.forEach { language ->
        val pref = slackConfig.preferences.find { it.languageTag == language.tag } ?: return@forEach
        if (pref.events.contains(SlackEventType.ALL) || pref.events.contains(SlackEventType.BASE_CHANGED)) {
          return true
        }
      }
      return false
    }

    return false
  }

  private fun shouldProcessEventNewKeyAdded(
    context: SlackMessageContext,
    modifiedLangTag: String,
    tag: String,
  ): Boolean {
    val slackConfig = context.slackConfig

    return if (slackConfig.isGlobalSubscription) {
      slackConfig.events.contains(SlackEventType.NEW_KEY) || slackConfig.events.contains(SlackEventType.ALL)
    } else {
      val pref =
        slackConfig.preferences.find { it.languageTag == modifiedLangTag } ?: return modifiedLangTag == tag
      pref.events.contains(SlackEventType.NEW_KEY) || pref.events.contains(SlackEventType.ALL)
    }
  }

  fun createAttachmentForLanguage(
    context: SlackMessageContext,
    langTag: String,
    keyId: Long,
    author: String?,
  ): Attachment? {
    val translation = context.dataProvider.getTranslation(keyId, langTag) ?: return null
    return createAttachmentForLanguage(context, translation, author)
  }

  private fun createAttachmentForLanguage(
    context: SlackMessageContext,
    translation: SlackTranslationInfoDto,
    author: String?,
  ): Attachment? {
    val baseLanguage = context.slackConfig.project.baseLanguage ?: return null

    if (context.shouldSkipModification(translation.languageTag)) {
      return null
    }

    val color = determineColorByState(translation.state)
    val blocksBody =
      if (translation.text != null) {
        blocksProvider.getBlocksWithTranslation(context, translation, author)
      } else {
        blocksProvider.getBlocksEmptyTranslation(context, translation)
      }
    return Attachment.builder()
      .color(color)
      .blocks(blocksBody)
      .fallback(translation.text ?: "")
      .build()
  }

  private fun determineColorByState(state: TranslationState?): String {
    return when (state) {
      TranslationState.TRANSLATED -> "#FFCE00"
      TranslationState.UNTRANSLATED -> "#BCC2CB"
      TranslationState.REVIEWED -> "#00B962"
      else -> "#BCC2CB"
    }
  }

  private fun getUrlOnImport(context: SlackMessageContext) =
    "${tolgeeProperties.frontEndUrl}/projects/${context.slackConfig.project.id}/" +
      "activity-detail?activity=${context.activityData?.revisionId}"

  private fun getUrlOnSpecifiedKey(
    context: SlackMessageContext,
    keyId: Long,
  ) = "${tolgeeProperties.frontEndUrl}/projects/${context.slackConfig.project.id}/" +
    "translations/single?id=$keyId"

  fun createMessageIfTooManyTranslations(
    context: SlackMessageContext,
    counts: Long,
  ): SlackMessageDto {
    return SlackMessageDto(
      blocks = blocksProvider.getBlocksTooManyTranslations(context, counts),
      attachments = listOf(blocksProvider.getRedirectButtonAttachment(getUrlOnImport(context))),
      0L,
      setOf(),
      false,
    )
  }
}
