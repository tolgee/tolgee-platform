package io.tolgee.ee.component.slackIntegration.notification.messageFactory

import com.slack.api.model.Attachment
import io.tolgee.api.IModifiedEntityModel
import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageContext
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageUrlProvider
import io.tolgee.ee.component.slackIntegration.notification.SlackNotificationBlocksProvider
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class SlackOnKeyAddedMessageFactory(
  private val i18n: I18n,
  private val slackMessageUrlProvider: SlackMessageUrlProvider,
  private val blocksProvider: SlackNotificationBlocksProvider,
) {
  fun createKeyAddMessages(context: SlackMessageContext): List<SlackMessageDto> {
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

      val attachment = blocksProvider.createAttachmentForLanguage(context, translation, null) ?: return@translations

      attachments.add(attachment)
      langTags.add(translation.languageTag)
    }

    if (!langTags.contains(context.baseLanguage.tag) && langTags.isNotEmpty()) {
      keyTranslations.find { it.languageId == context.baseLanguage.id }?.let { baseTranslation ->
        val attachment = blocksProvider.createAttachmentForLanguage(context, baseTranslation, null) ?: return@let

        attachments.add(attachment)
        langTags.add(context.baseLanguage.tag)
      }
    }

    attachments.add(
      blocksProvider.getRedirectButtonAttachment(
        slackMessageUrlProvider.getUrlOnSpecifiedKey(context, key.id),
      ),
    )

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
}
