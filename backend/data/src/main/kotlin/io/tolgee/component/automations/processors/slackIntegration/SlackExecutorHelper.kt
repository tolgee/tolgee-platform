package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackConfigPreference
import io.tolgee.model.translation.Translation
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.slackIntegration.SlackSubscriptionService
import io.tolgee.util.I18n
import kotlin.jvm.optionals.getOrElse

class SlackExecutorHelper(
  val slackConfig: SlackConfig,
  val data: SlackRequest,
  val keyService: KeyService,
  val permissionService: PermissionService,
  private val slackSubscriptionService: SlackSubscriptionService,
  private val i18n: I18n,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun createKeyAddMessage(): SavedMessageDto? {
    val activities = data.activityData ?: return null
    val attachments = mutableListOf<Attachment>()
    val langTags: MutableSet<String> = mutableSetOf()
    var keyId: Long = 0
    var blocksHeader: List<LayoutBlock> = listOf()
    val baseLanguage = slackConfig.project.baseLanguage ?: return null
    // Extracting Key and Translation Information
    val modifiedEntities = activities.modifiedEntities ?: return null
    modifiedEntities.forEach modifiedEntities@{ (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach modifiedEntitiesList@{ modifiedEntity ->
        when (entityType) {
          "Key" -> {
            keyId = modifiedEntity.entityId
            val key = keyService.get(keyId)
            blocksHeader = buildKeyInfoBlock(key, i18n.translate("new-key-text"))
            key.translations.forEach translations@{ translation ->
              if (!shouldProcessEvent(
                  translation.language.tag,
                  baseLanguage.tag,
                  EventName.NEW_KEY,
                )
              ) {
                return@translations
              }

              val attachment = createAttachmentForLanguage(translation) ?: return@translations

              attachments.add(attachment)
              langTags.add(translation.language.tag)
            }
          }
        }
      }
    }
    slackConfig.project.languages.forEach { language ->
      if (!langTags.contains(language.tag)) {
        if (!shouldProcessEvent(
            language.tag,
            baseLanguage.tag,
            EventName.NEW_KEY,
          )
        ) {
          return@forEach
        }
        val blocks = buildBlocksNoTranslation(baseLanguage, language)
        attachments.add(
          Attachment.builder()
            .color("#BCC2CB")
            .blocks(blocks)
            .build(),
        )
        langTags.add(language.tag)
      }
    }

    if (!langTags.contains(baseLanguage.tag) && langTags.isNotEmpty()) {
      baseLanguage.translations?.find { it.key.id == keyId }?.let { baseTranslation ->
        val attachment = createAttachmentForLanguage(baseTranslation) ?: return@let

        attachments.add(attachment)
        langTags.add(baseLanguage.tag)
      }
    }

    attachments.add(createRedirectButton())

    if (attachments.isEmpty() || blocksHeader.isEmpty()) {
      return null
    }
    return SavedMessageDto(
      blocks = blocksHeader,
      attachments = attachments,
      keyId = keyId,
      langTag = langTags,
    )
  }

  private fun buildBlocksNoTranslation(
    baseLanguage: Language,
    language: Language,
  ) = withBlocks {
    if (shouldSkipModification(
        slackConfig.preferences,
        language.tag,
        baseLanguage,
        slackConfig.isGlobalSubscription,
      )
    ) {
      return@withBlocks
    }
    val languageName = language.name
    val flagEmoji = language.flagEmoji
    val ifBase =
      if (baseLanguage.id == language.id) {
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
    translation: Translation,
    baseLanguage: Language,
  ) = withBlocks {
    if (shouldSkipModification(
        slackConfig.preferences,
        translation.language.tag,
        baseLanguage,
        slackConfig.isGlobalSubscription,
      )
    ) {
      return@withBlocks
    }
    section {
      languageInfoSection(baseLanguage, translation.language)
    }

    section {
      val currentTranslate = translation.text!!
      markdownText(currentTranslate)
    }
  }

  private fun buildKeyInfoBlock(
    key: Key,
    head: String,
  ) = withBlocks {
    section {
      val nickname = slackSubscriptionService.getBySlackId(slackConfig.slackId)?.slackNickName ?: return@section
      markdownText("@$nickname $head")
    }

    section {
      markdownText("*Key:* ${key.name}")
    }

    section {
      markdownText("*Key namespace:* ${key.namespace ?: "None"}")
    }
  }

  fun createTranslationChangeMessage(): SavedMessageDto? {
    var result: SavedMessageDto? = null

    data.activityData?.modifiedEntities?.forEach modifiedEntities@{ (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        modifiedEntity.modifications?.forEach modificationLoop@{ (property, modification) ->
          if (property != "text" && property != "state") {
            return@modificationLoop
          }
          val translationKey = modifiedEntity.entityId
          result = processTranslationChange(translationKey)
          if (result != null) {
            return@modifiedEntities
          }
        }
      }
    }

    return result
  }

  private fun processTranslationChange(translationKey: Long): SavedMessageDto? {
    val translation = findTranslationByKey(translationKey) ?: return null
    val key = translation.key
    val baseLanguageTag = slackConfig.project.baseLanguage?.tag ?: return null
    val modifiedLangTag = translation.language.tag

    if (!shouldProcessEvent(modifiedLangTag, baseLanguageTag, EventName.TRANSLATION_CHANGED)) return null

    val langName =
      if (translation.language.tag == baseLanguageTag) {
        "(base language)"
      } else {
        "(${translation.language.name})"
      }

    val headerBlock = buildKeyInfoBlock(key, i18n.translate("new-translation-text") + langName)
    val attachments = mutableListOf(createAttachmentForLanguage(translation) ?: return null)
    val langTags = mutableSetOf(modifiedLangTag)

    addLanguagesIfNeed(attachments, langTags, translation.key.id, modifiedLangTag, baseLanguageTag)
    attachments.add(createRedirectButton())

    return if (headerBlock.isEmpty()) {
      null
    } else {
      SavedMessageDto(
        blocks = headerBlock,
        attachments = attachments,
        keyId = key.id,
        langTag = langTags,
      )
    }
  }

  private fun addLanguagesIfNeed(
    attachments: MutableList<Attachment>,
    langTags: MutableSet<String>,
    keyId: Long,
    modifiedLangTag: String,
    baseLanguageTag: String,
  ) {
    slackConfig.project.languages.forEach { language ->
      if (shouldAddLanguage(langTags, modifiedLangTag, language.tag, baseLanguageTag)) {
        createAttachmentForLanguage(language.tag, keyId)?.let { attachment ->
          attachments.add(attachment)
          langTags.add(language.tag)
        }
      }
    }
  }

  private fun shouldAddLanguage(
    langTags: Set<String>,
    modifiedLangTag: String,
    currentLangTag: String,
    baseLanguageTag: String,
  ): Boolean =
    !langTags.contains(currentLangTag) &&
      (modifiedLangTag == baseLanguageTag || currentLangTag == baseLanguageTag)

  private fun findTranslationByKey(translationKey: Long): Translation? =
    keyService.find(slackConfig.project.id, translationKey).getOrElse { null }?.let { key ->
      key.translations.find { it.id == translationKey }
    }

  private fun determineColorByState(state: TranslationState?): String {
    return when (state) {
      TranslationState.TRANSLATED -> "#FFCE00"
      TranslationState.UNTRANSLATED -> "#BCC2CB"
      TranslationState.REVIEWED -> "#00B962"
      else -> "#BCC2CB"
    }
  }

  private fun SectionBlockBuilder.languageInfoSection(
    baseLanguage: Language,
    language: Language,
  ) {
    val languageName = language.name
    val flagEmoji = language.flagEmoji
    val ifBase =
      if (baseLanguage.id == language.id) {
        "(base)"
      } else {
        ""
      }

    markdownText("$flagEmoji *$languageName* $ifBase")
  }

  private fun shouldSkipModification(
    preferences: Set<SlackConfigPreference>,
    langTag: String,
    baseLanguage: Language,
    globalSubscription: Boolean,
  ): Boolean {
    val languageTagsSet = preferences.map { it.languageTag }.toSet()
    return !globalSubscription &&
      !languageTagsSet.contains(langTag) &&
      baseLanguage.tag != langTag
  }

  private fun shouldProcessEvent(
    modifiedLangTag: String,
    baseLanguageTag: String,
    event: EventName,
  ): Boolean {
    val isBaseChanged = modifiedLangTag == baseLanguageTag

    val eventHandlingPreferences =
      if (slackConfig.isGlobalSubscription) {
        Triple(
          slackConfig.onEvent == EventName.ALL,
          slackConfig.onEvent == EventName.BASE_CHANGED && isBaseChanged,
          slackConfig.onEvent == EventName.TRANSLATION_CHANGED && !isBaseChanged,
        )
      } else {
        slackConfig.preferences.find { it.languageTag == modifiedLangTag }?.let { pref ->
          Triple(
            pref.onEvent == EventName.ALL,
            pref.onEvent == EventName.BASE_CHANGED && isBaseChanged,
            pref.onEvent == EventName.TRANSLATION_CHANGED && !isBaseChanged,
          )
        } ?: return false // Возвращаем false, если настройки для языка не найдены.
      }

    return when (event) {
      EventName.TRANSLATION_CHANGED -> {
        val (isAllEvent, isBaseLanguageChangedEvent, isTranslationChangedEvent) = eventHandlingPreferences
        isAllEvent || isBaseLanguageChangedEvent || isTranslationChangedEvent
      }
      EventName.NEW_KEY -> {
        if (slackConfig.isGlobalSubscription) {
          slackConfig.onEvent == event || slackConfig.onEvent == EventName.ALL
        } else {
          val pref = slackConfig.preferences.find { it.languageTag == modifiedLangTag } ?: return false
          pref.onEvent == EventName.NEW_KEY || pref.onEvent == EventName.ALL
        }
      }
      else -> false
    }
  }

  fun createAttachmentForLanguage(
    langTag: String,
    keyId: Long,
  ): Attachment? {
    val result =
      keyService.find(keyId)?.let { foundKey ->
        val translation = foundKey.translations.find { it.language.tag == langTag }
        val baseLanguage = slackConfig.project.baseLanguage ?: return null

        if (shouldSkipModification(slackConfig.preferences, langTag, baseLanguage, slackConfig.isGlobalSubscription)) {
          return null
        }

        val color = determineColorByState(translation?.state)
        val blocksBody =
          if (translation?.text != null) {
            buildBlocksWithTranslation(translation, baseLanguage)
          } else {
            val language = slackConfig.project.languages.find { it.tag == langTag } ?: return null
            buildBlocksNoTranslation(baseLanguage, language)
          }

        Attachment.builder()
          .color(color)
          .blocks(blocksBody)
          .fallback("New key added to Tolgee project")
          .build()
      } ?: return null

    return result
  }

  private fun createAttachmentForLanguage(translation: Translation): Attachment? {
    val baseLanguage = slackConfig.project.baseLanguage ?: return null

    val savedLanguageTags = slackConfig.preferences
    if (shouldSkipModification(
        savedLanguageTags,
        translation.language.tag,
        baseLanguage,
        slackConfig.isGlobalSubscription,
      )
    ) {
      return null
    }

    val color = determineColorByState(translation.state)
    val blocksBody =
      if (translation.text != null) {
        buildBlocksWithTranslation(translation, baseLanguage)
      } else {
        buildBlocksNoTranslation(baseLanguage, translation.language)
      }
    return Attachment.builder()
      .color(color)
      .blocks(blocksBody)
      .fallback("New key added to Tolgee project")
      .build()
  }

  private fun createRedirectButton() =
    Attachment.builder()
      .blocks(
        withBlocks {
          actions {
            redirectOnPlatformButton()
          }
        },
      )
      .color("#EC407A")
      .build()

  private fun ActionsBlockBuilder.redirectOnPlatformButton() {
    val tolgeeUrl = "${tolgeeProperties.frontEndUrl}/projects/${slackConfig.project.id}/translations"

    button {
      text(i18n.translate("tolgee_redirect_button_text"), emoji = true)
      value("redirect")
      url(tolgeeUrl)
      actionId("button_redirect_to_tolgee")
    }
  }
}
