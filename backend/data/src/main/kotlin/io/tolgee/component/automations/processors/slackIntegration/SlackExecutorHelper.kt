package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.model.kotlin_extension.view.blocks
import com.slack.api.model.view.View
import com.slack.api.model.view.Views.view
import com.slack.api.model.view.Views.viewTitle
import io.tolgee.activity.data.PropertyModification
import io.tolgee.model.Language
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.SlackConfig
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
) {
  fun buildSuccessView(): View {
    return view { thisView ->
      thisView
        .callbackId("callback")
        .type("modal")
        .title(viewTitle { it.type("plain_text").text("Result").emoji(true) })
        .blocks {
          section {
            markdownText("*New translation was added!*")
          }
        }
    }
  }

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
        slackConfig.languageTags,
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
        slackConfig.languageTags,
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
    val keyId: Long
    val langTags: MutableSet<String> = mutableSetOf()
    var headerBlock: List<LayoutBlock> = emptyList()
    val attachments = mutableListOf<Attachment>()
    val isBaseLanguageChangedEvent = slackConfig.onEvent == EventName.BASE_CHANGED
    val isAllEvent = slackConfig.onEvent == EventName.ALL

    keyService.find(slackConfig.project.id, translationKey).getOrElse { return null }.apply {
      keyId = this.id
      val translation = translations.find { it.id == translationKey }
      val baseLanguage = slackConfig.project.baseLanguage ?: return null
      val lang = translation?.language?.tag ?: return null

      val isBaseLanguage = baseLanguage.id == translation.language.id
      if (!isAllEvent && shouldSkipEvent(isBaseLanguageChangedEvent, isBaseLanguage)) {
        return null
      }

      val attachment = createAttachmentForLanguage(translation) ?: return null
      val langName =
        if (isBaseLanguage) {
          "(base language)"
        } else {
          "(" + translation.language.name + ")"
        }
      headerBlock = buildKeyInfoBlock(this, i18n.translate("new-translation-text") + langName)

      attachments.add(attachment)

      langTags.add(lang)
    }

    return if (attachments.isEmpty() || headerBlock.isEmpty()) {
      null
    } else {
      SavedMessageDto(
        blocks = headerBlock,
        attachments = attachments,
        keyId = keyId,
        langTag = langTags,
      )
    }
  }

  private fun determineColorByState(state: TranslationState?): String {
    return when (state) {
      TranslationState.TRANSLATED -> "#FFCE00"
      TranslationState.UNTRANSLATED -> "#BCC2CB"
      TranslationState.REVIEWED -> "#00B962"
      else -> "#BCC2CB"
    }
  }

  private fun SectionBlockBuilder.modificationSection(
    entityType: String,
    property: String,
    modification: PropertyModification,
    translation: Translation?,
  ) {
    val oldValue = modification.old?.toString() ?: "None"
    val newValue = modification.new?.toString() ?: "None"
    val langInfo = translation?.language?.let { "*Language:* ${it.name} ${it.flagEmoji}" } ?: ""
    markdownText("*$entityType $property* $langInfo \n  ~$oldValue~ -> $newValue")
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
    savedLanguageTags: Set<String>,
    langTag: String,
    baseLanguage: Language,
    globalSubscription: Boolean,
  ): Boolean =
    !globalSubscription &&
      !savedLanguageTags.contains(
        langTag,
      ) && baseLanguage.tag != langTag

  private fun shouldSkipEvent(
    isBaseLanguageChangedEvent: Boolean,
    isBaseLanguage: Boolean,
  ): Boolean {
    return (isBaseLanguageChangedEvent && !isBaseLanguage) || (!isBaseLanguageChangedEvent && isBaseLanguage)
  }

  private fun shouldSkipInput(
    translation: Translation?,
    langTags: Set<String>,
  ) = translation?.language == null ||
    (slackConfig.isGlobalSubscription && (langTags.isEmpty() || !langTags.contains(translation.language.tag)))

  private fun hasPermissions(
    projectId: Long,
    userAccountId: Long,
  ): Boolean =
    permissionService.getProjectPermissionScopes(projectId, userAccountId)?.contains(Scope.TRANSLATIONS_EDIT) == true

  fun createAttachmentForLanguage(
    lang: String,
    keyId: Long,
  ): Attachment? {
    val result =
      keyService.find(keyId)?.let { foundKey ->
        val translation = foundKey.translations.find { it.language.tag == lang }
        val baseLanguage = slackConfig.project.baseLanguage ?: return null

        val savedLanguageTags = slackConfig.languageTags
        if (shouldSkipModification(savedLanguageTags, lang, baseLanguage, slackConfig.isGlobalSubscription)) {
          return null
        }

        val color = determineColorByState(translation?.state)
        val blocksBody =
          if (translation?.text != null) {
            buildBlocksWithTranslation(translation, baseLanguage)
          } else {
            val language = slackConfig.project.languages.find { it.tag == lang } ?: return null
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

    val savedLanguageTags = slackConfig.languageTags
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
}
