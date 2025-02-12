package io.tolgee.ee.component.slackIntegration

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.api.IModifiedEntityModel
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.component.slackIntegration.data.KeyInfoDto
import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.component.slackIntegration.data.TranslationInfoDto
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackConfigPreference
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.service.language.LanguageService
import io.tolgee.util.I18n
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*

class SlackExecutorHelper(
  val applicationContext: ApplicationContext,
  val slackConfig: SlackConfig,
  val data: SlackRequest,
  private val i18n: I18n,
  private val author: String?,
) : Logging {
  fun createKeyAddMessage(): List<SlackMessageDto> {
    val activities = data.activityData ?: return emptyList()

    val messages: MutableList<SlackMessageDto> = mutableListOf()
    val modifiedEntities = activities.modifiedEntities ?: return emptyList()
    modifiedEntities.flatMap { (entityType, modifiedEntityList) ->
      modifiedEntityList.map modifiedEntitiesList@{ modifiedEntity ->
        when (entityType) {
          "Key" -> {
            createMessageForAddKey(modifiedEntity)?.let { messages.add(it) }
          }

          else -> {
            return@modifiedEntitiesList
          }
        }
      }
    }

    return messages
  }

  private fun createMessageForAddKey(modifiedEntity: IModifiedEntityModel): SlackMessageDto? {
    val attachments = mutableListOf<Attachment>()
    val langTags: MutableSet<String> = mutableSetOf()

    val keyId = modifiedEntity.entityId
    val key = dataProvider.getKeyInfo(keyId)
    val blocksHeader: List<LayoutBlock> = buildKeyInfoBlock(key, i18n.translate("slack.common.message.new-key"))
    val keyTranslations = dataProvider.getKeyTranslations(key.id)

    keyTranslations.forEach translations@{ translation ->
      if (!shouldProcessEventNewKeyAdded(translation.languageTag, baseLanguage.tag)) {
        return@translations
      }

      val attachment = createAttachmentForLanguage(translation, null) ?: return@translations

      attachments.add(attachment)
      langTags.add(translation.languageTag)
    }

    if (!langTags.contains(baseLanguage.tag) && langTags.isNotEmpty()) {
      keyTranslations.find { it.languageId == baseLanguage.id }?.let { baseTranslation ->
        val attachment = createAttachmentForLanguage(baseTranslation, null) ?: return@let

        attachments.add(attachment)
        langTags.add(baseLanguage.tag)
      }
    }

    attachments.add(createRedirectButton(getUrlOnSpecifiedKey(key.id)))

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

  private fun buildBlocksEmptyTranslation(translation: TranslationInfoDto) =
    withBlocks {
      if (shouldSkipModification(
          slackConfig.preferences,
          translation.languageTag,
          slackConfig.isGlobalSubscription,
        )
      ) {
        return@withBlocks
      }
      val languageName = translation.languageName
      val flagEmoji = translation.languageFlagEmoji
      val ifBase =
        if (baseLanguage.id == translation.languageId) {
          "(base)"
        } else {
          ""
        }

      section {
        markdownText("$flagEmoji *$languageName* $ifBase")
      }

      context {
        markdownText("No translation")
      }
    }

  private fun buildBlocksWithTranslation(
    translation: TranslationInfoDto,
    author: String?,
  ) = withBlocks {
    if (shouldSkipModification(
        slackConfig.preferences,
        translation.languageTag,
        slackConfig.isGlobalSubscription,
      )
    ) {
      return@withBlocks
    }
    section {
      languageInfoSection(translation)
    }

    section {
      val currentTranslate = translation.text!!
      markdownText(currentTranslate)
    }
    val contextText = author ?: return@withBlocks
    if (contextText.isEmpty()) {
      return@withBlocks
    }
    context {
      markdownText(contextText)
    }
  }

  private fun buildKeyInfoBlock(
    key: KeyInfoDto,
    head: String,
  ) = withBlocks {
    section {
      authorHeadSection(head)
    }

    val columnFields = mutableListOf<Pair<String, String?>>()
    columnFields.add("Key" to key.name)
    key.tags?.let { tags ->
      val tagNames = tags.joinToString(", ")
      if (tagNames.isNotBlank()) {
        columnFields.add("Tags" to tagNames)
      }
    }
    columnFields.add("Namespace" to key.namespace)
    columnFields.add("Description" to key.description)

    field(columnFields)
  }

  fun LayoutBlockDsl.field(keyValue: List<Pair<String, String?>>) {
    section {
      val filtered = keyValue.filter { it.second != null && it.second!!.isNotEmpty() }

      if (filtered.isEmpty()) return@section
      fields {
        filtered.forEachIndexed { index, (key, value) ->
          val finalValue = value + if (index % 2 == 1 && index != filtered.size - 1) "\n\u200d" else ""
          markdownText("*$key* \n$finalValue")
        }
      }
    }
  }

  fun createImportMessage(): SlackMessageDto? {
    val importedCount = data.activityData?.counts?.get("Key") ?: return null

    return SlackMessageDto(
      blocks = buildImportBlocks(importedCount),
      attachments = listOf(createRedirectButton(getUrlOnImport())),
      0L,
      setOf(),
    )
  }

  private fun buildImportBlocks(count: Long) =
    withBlocks {
      section {
        authorHeadSection(i18n.translate("slack.common.message.imported") + " $count keys")
      }
    }

  private fun buildBlocksTooManyTranslations(count: Long) =
    withBlocks {
      section {
        authorHeadSection(i18n.translate("slack.common.message.too-many-translations").format(count))
      }
    }

  fun createTranslationChangeMessage(): MutableList<SlackMessageDto> {
    val result: MutableList<SlackMessageDto> = mutableListOf()

    data.activityData?.modifiedEntities?.forEach modifiedEntities@{ (_, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        val event =
          when {
            modifiedEntity.modifications?.contains("text") == true -> "translation"
            modifiedEntity.modifications?.contains("state") == true -> "state"
            else -> ""
          }

        val translationId = modifiedEntity.entityId
        val translation = dataProvider.getTranslationById(translationId) ?: return@modifiedEntities
        result.add(
          processTranslationChange(
            translation,
            getModificationAuthorContext(event, data.activityData.timestamp),
            event,
          ) ?: return@modifiedEntities,
        )

        val baseLanguageTag = slackConfig.project.baseLanguage?.tag ?: return@modifiedEntities
        if (baseLanguageTag == translation.languageTag) {
          return@modifiedEntities
        }
      }
    }

    return result
  }

  private fun processTranslationChange(
    translation: TranslationInfoDto,
    modificationAuthor: String?,
    event: String,
  ): SlackMessageDto? {
    val baseLanguageTag = slackConfig.project.baseLanguage?.tag ?: return null
    val modifiedLangTag = translation.languageTag
    val isBaseChanged = modifiedLangTag == baseLanguageTag

    if (!shouldProcessEventTranslationChanged(modifiedLangTag, baseLanguageTag, modifiedLangTag)) return null

    val langName =
      if (isBaseChanged) {
        "base language"
      } else {
        translation.languageName
      }

    val key = dataProvider.getKeyInfo(translation.keyId)

    val headerBlock =
      buildKeyInfoBlock(key, i18n.translate("slack.common.message.new-translation").format(langName, event))
    val attachments =
      mutableListOf(createAttachmentForLanguage(translation, modificationAuthor) ?: return null)
    val langTags = mutableSetOf(modifiedLangTag)

    addLanguagesIfNeed(attachments, langTags, key.id, modifiedLangTag, baseLanguageTag)
    attachments.add(createRedirectButton(getUrlOnSpecifiedKey(key.id)))

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
    event: String,
    timestamp: Long,
  ): String {
    val authorMention = author ?: data.activityData?.author?.name
    val correctTimestamp = timestamp / 1000 // Convert milliseconds to seconds

    // fallback to timestamp in case of any error
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = formatter.format(date)

    val formattedTimestamp = "<!date^$correctTimestamp^{time}|$formattedTime>"
    return i18n.translate("slack.common.message.modification-info").format(authorMention, event, formattedTimestamp)
  }

  private fun addLanguagesIfNeed(
    attachments: MutableList<Attachment>,
    addedTags: MutableSet<String>,
    keyId: Long,
    modifiedLangTag: String,
    baseLanguageTag: String,
  ) {
    slackConfig.project.languages.forEach { language ->
      if (!shouldProcessEventTranslationChanged(
          modifiedLangTag,
          baseLanguageTag,
          language.tag,
        ) && language.tag != baseLanguageTag
      ) {
        return@forEach
      }

      if (shouldAddLanguage(addedTags, modifiedLangTag, language.tag, baseLanguageTag)) {
        createAttachmentForLanguage(language.tag, keyId, null)?.let { attachment ->
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

  private fun determineColorByState(state: TranslationState?): String {
    return when (state) {
      TranslationState.TRANSLATED -> "#FFCE00"
      TranslationState.UNTRANSLATED -> "#BCC2CB"
      TranslationState.REVIEWED -> "#00B962"
      else -> "#BCC2CB"
    }
  }

  private fun SectionBlockBuilder.languageInfoSection(translation: TranslationInfoDto) {
    val languageName = translation.languageName
    val flagEmoji = translation.languageFlagEmoji
    val ifBase =
      if (baseLanguage.id == translation.languageId) {
        "(base)"
      } else {
        ""
      }

    markdownText("$flagEmoji *$languageName* $ifBase")
  }

  private fun SectionBlockBuilder.authorHeadSection(head: String) {
    val authorMention = author ?: data.activityData?.author?.name
    markdownText(" *$authorMention* $head")
  }

  private fun shouldSkipModification(
    preferences: Set<SlackConfigPreference>,
    langTag: String,
    globalSubscription: Boolean,
  ): Boolean {
    val languageTagsSet = preferences.map { it.languageTag }.toSet()
    return !globalSubscription &&
      !languageTagsSet.contains(langTag) &&
      baseLanguage.tag != langTag
  }

  private fun shouldProcessEventTranslationChanged(
    modifiedLangTag: String,
    baseLanguageTag: String,
    currentLangTag: String,
  ): Boolean {
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
    modifiedLangTag: String,
    tag: String,
  ): Boolean {
    return if (slackConfig.isGlobalSubscription) {
      slackConfig.events.contains(SlackEventType.NEW_KEY) || slackConfig.events.contains(SlackEventType.ALL)
    } else {
      val pref = slackConfig.preferences.find { it.languageTag == modifiedLangTag } ?: return modifiedLangTag == tag
      pref.events.contains(SlackEventType.NEW_KEY) || pref.events.contains(SlackEventType.ALL)
    }
  }

  fun createAttachmentForLanguage(
    langTag: String,
    keyId: Long,
    author: String?,
  ): Attachment? {
    val translation = dataProvider.getTranslation(keyId, langTag) ?: return null
    return createAttachmentForLanguage(translation, author)
  }

  private fun createAttachmentForLanguage(
    translation: TranslationInfoDto,
    author: String?,
  ): Attachment? {
    val baseLanguage = slackConfig.project.baseLanguage ?: return null

    val languagePreferences = slackConfig.preferences
    if (shouldSkipModification(
        languagePreferences,
        translation.languageTag,
        slackConfig.isGlobalSubscription,
      )
    ) {
      return null
    }

    val color = determineColorByState(translation.state)
    val blocksBody =
      if (translation.text != null) {
        buildBlocksWithTranslation(translation, author)
      } else {
        buildBlocksEmptyTranslation(translation)
      }
    return Attachment.builder()
      .color(color)
      .blocks(blocksBody)
      .fallback(translation.text ?: "")
      .build()
  }

  private fun createRedirectButton(url: String) =
    Attachment.builder()
      .blocks(
        withBlocks {
          actions {
            logger.trace("URL: $url")
            redirectOnPlatformButton(url)
          }
        },
      )
      .color("#00000000")
      .build()

  private fun ActionsBlockBuilder.redirectOnPlatformButton(tolgeeUrl: String) {
    button {
      text(i18n.translate("slack.common.text.button.tolgee_redirect"), emoji = true)
      value("redirect")
      url(tolgeeUrl)
      actionId("button_redirect_to_tolgee")
      style("danger")
    }
  }

  private fun getUrlOnImport() =
    "${tolgeeProperties.frontEndUrl}/projects/${slackConfig.project.id}/" +
      "activity-detail?activity=${data.activityData?.revisionId}"

  private fun getUrlOnSpecifiedKey(keyId: Long) =
    "${tolgeeProperties.frontEndUrl}/projects/${slackConfig.project.id}/" +
      "translations/single?id=$keyId"

  fun createMessageIfTooManyTranslations(counts: Long): SlackMessageDto {
    return SlackMessageDto(
      blocks = buildBlocksTooManyTranslations(counts),
      attachments = listOf(createRedirectButton(getUrlOnImport())),
      0L,
      setOf(),
      false,
    )
  }

  private val tolgeeProperties by lazy {
    applicationContext.getBean(TolgeeProperties::class.java)
  }

  private val dataProvider by lazy {
    applicationContext.getBean(SlackIntegrationDataProvider::class.java)
  }

  private val languageService by lazy {
    applicationContext.getBean(LanguageService::class.java)
  }

  private val baseLanguage by lazy {
    languageService.getProjectBaseLanguage(slackConfig.project.id)
  }
}
