package io.tolgee.ee.component.slackIntegration.notification.messageFactory

import com.slack.api.model.Attachment
import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.data.SlackTranslationInfoDto
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageContext
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageUrlProvider
import io.tolgee.ee.component.slackIntegration.notification.SlackNotificationBlocksProvider
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.util.I18n
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Component
class SlackTranslationChangeMessageFactory(
  private val i18n: I18n,
  private val slackMessageUrlProvider: SlackMessageUrlProvider,
  private val blocksProvider: SlackNotificationBlocksProvider,
) {
  fun createTranslationChangeMessages(context: SlackMessageContext): List<SlackMessageDto> {
    val result = mutableListOf<SlackMessageDto>()

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

        val message =
          processTranslationChange(
            context,
            translation,
            getModificationAuthorContext(context, event, activityData.timestamp),
            event,
          ) ?: return@modifiedEntities

        result.add(message)

        val baseLanguageTag =
          context.slackConfig.project.baseLanguage
            ?.tag ?: return@modifiedEntities
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
    val baseLanguageTag =
      context.slackConfig.project.baseLanguage
        ?.tag ?: return null
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
      mutableListOf(blocksProvider.createAttachmentForLanguage(context, translation, modificationAuthor) ?: return null)
    val langTags = mutableSetOf(modifiedLangTag)

    addLanguagesIfNeed(context, attachments, langTags, key.id, modifiedLangTag, baseLanguageTag)
    attachments.add(
      blocksProvider.getRedirectButtonAttachment(
        slackMessageUrlProvider.getUrlOnSpecifiedKey(
          context,
          key.id,
        ),
      ),
    )

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
        ) &&
        language.tag != baseLanguageTag
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

  fun createAttachmentForLanguage(
    context: SlackMessageContext,
    langTag: String,
    keyId: Long,
    author: String?,
  ): Attachment? {
    val translation = context.dataProvider.getTranslation(keyId, langTag) ?: return null
    return blocksProvider.createAttachmentForLanguage(context, translation, author)
  }

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
}
