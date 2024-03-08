package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.InputBlockBuilder
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.model.kotlin_extension.view.blocks
import com.slack.api.model.view.View
import com.slack.api.model.view.Views.view
import com.slack.api.model.view.Views.viewTitle
import io.tolgee.activity.data.PropertyModification
import io.tolgee.constants.SlackEventActions
import io.tolgee.model.Language
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.translation.Translation
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.PermissionService
import kotlin.jvm.optionals.getOrElse

class SlackExecutorHelper(
  val slackConfig: SlackConfig,
  val data: SlackRequest,
  val keyService: KeyService,
  val permissionService: PermissionService,
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

    // Extracting Key and Translation Information
    val modifiedEntities = activities.modifiedEntities ?: return null
    modifiedEntities.forEach modifiedEntities@{ (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach modifiedEntitiesList@{ modifiedEntity ->
        when (entityType) {
          "Key" -> {
            keyId = modifiedEntity.entityId
            val key = keyService.get(keyId)
            val baseLanguage = slackConfig.project.baseLanguage ?: return@modifiedEntitiesList

            blocksHeader = buildKeyInfoBlock(key)

            key.translations.forEach translations@{ translation ->
              val blocksBody = buildBodyForNewKey(translation, baseLanguage, keyId)
              val color = determineColorByState(translation.state)
              attachments.add(
                Attachment.builder()
                  .color(color)
                  .blocks(blocksBody.toList())
                  .fallback("New key added to Tolgee project")
                  .build(),
              )
              langTags.add(translation.language.tag)
            }
          }
        }
      }
    }

    // Check if attachments list is empty
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

  private fun buildBodyForNewKey(
    translation: Translation,
    baseLanguage: Language,
    keyId: Long,
  ) = withBlocks {
    if (shouldSkipModification(slackConfig.languageTags, translation, baseLanguage, slackConfig.isGlobalSubscription)) {
      return@withBlocks
    }

    val language = translation.language
    val languageName = language.name
    val flagEmoji = language.flagEmoji
    val stateModification = translation.state.name
    divider()
    section {
      markdownText("*Translation State:* $stateModification\n*Language:* $languageName $flagEmoji")
    }

    section {
      val currentTranslate = translation.text ?: "None"
      markdownText("*Current translate:* $currentTranslate")
    }

    val shouldSkip = shouldSkipInput(translation, slackConfig.languageTags)
    if (shouldSkip ||
      !hasPermissions(slackConfig.project.id, slackConfig.userAccount.id)
    ) {
      return@withBlocks
    }
    input {
      translateInput(keyId, language)
    }
  }

  private fun buildKeyInfoBlock(key: Key) =
    withBlocks {
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
          result = processTranslationChange(translationKey, modification, entityType, property)
          if (result != null) {
            return@modifiedEntities
          }
        }
      }
    }

    return result
  }

  private fun processTranslationChange(
    translationKey: Long,
    modification: PropertyModification,
    entityType: String,
    property: String,
  ): SavedMessageDto? {
    val keyId: Long
    val langTags: MutableSet<String> = mutableSetOf()
    var headerBlock: List<LayoutBlock> = emptyList()
    var footerBlock: List<LayoutBlock> = emptyList()
    val attachments = mutableListOf<Attachment>()
    val isBaseLanguageChangedEvent = slackConfig.onEvent == EventName.BASE_CHANGED
    val isAllEvent = slackConfig.onEvent == EventName.ALL

    keyService.find(slackConfig.project.id, translationKey).getOrElse { return null }.apply {
      keyId = this.id
      val translation = translations.find { it.id == translationKey }
      val baseLanguage = slackConfig.project.baseLanguage ?: return null
      val lang = translation?.language?.tag ?: return null
      val baseTranslation = translations.find { it.language.id == baseLanguage.id } ?: return null

      val isBaseLanguage = baseLanguage.id == translation.language.id
      if (!isAllEvent && shouldSkipEvent(isBaseLanguageChangedEvent, isBaseLanguage)) {
        return null
      }

      val savedLanguageTags = slackConfig.languageTags
      if (shouldSkipModification(savedLanguageTags, translation, baseLanguage, slackConfig.isGlobalSubscription)) {
        return null
      }

      val color = determineColorByState(translation.state)
      val bodyBlock = buildBlocksForTranslation(translation, this, modification, entityType, property)
      headerBlock = buildKeyInfoBlock(this)
      footerBlock =
        withBlocks {
          section {
            addBaseTranslationSection(baseTranslation, baseLanguage)
          }
          divider()
        }

      attachments.add(
        Attachment.builder()
          .color(color)
          .blocks(bodyBlock)
          .fallback("New update in Tolgee project for ${this.name}")
          .build(),
      )

      langTags.add(lang)
    }

    return if (attachments.isEmpty() || headerBlock.isEmpty() || footerBlock.isEmpty()) {
      null
    } else {
      SavedMessageDto(
        blocks = headerBlock + footerBlock,
        attachments = attachments,
        keyId = keyId,
        langTag = langTags,
      )
    }
  }

  private fun buildBlocksForTranslation(
    translation: Translation?,
    key: Key,
    modification: PropertyModification,
    entityType: String,
    property: String,
  ) = withBlocks {
    section {
      if (property == "text") {
        addModificationSection(entityType, property, modification, translation)
      } else {
        val langInfo = translation?.language?.let { "*Language:* ${it.name} ${it.flagEmoji}" } ?: ""
        markdownText("*State was changed* \n *Key translate: * ${translation?.text} \n $langInfo")
      }
    }

    val shouldSkip = shouldSkipInput(translation, slackConfig.languageTags)
    if (shouldSkip ||
      !hasPermissions(slackConfig.project.id, slackConfig.userAccount.id)
    ) {
      return@withBlocks
    }

    input {
      if (translation != null) {
        translateInput(key.id, translation.language)
      }
    }
  }

  private fun determineColorByState(state: TranslationState?): String {
    return when (state) {
      TranslationState.TRANSLATED -> "#B29109"
      TranslationState.UNTRANSLATED -> "#898989"
      TranslationState.REVIEWED -> "#177914"
      else -> "#cccccc"
    }
  }

  private fun InputBlockBuilder.translateInput(
    keyId: Long,
    language: Language,
  ) {
    dispatchAction(true)
    label(text = "Translate me ${language.flagEmoji}", emoji = true)
    plainTextInput {
      actionId(SlackEventActions.TRANSLATE_VALUE.name + "/$keyId" + "/${language.tag}")
      dispatchActionConfig {
        triggerActionsOn("on_enter_pressed")
      }
    }
  }

  private fun SectionBlockBuilder.addModificationSection(
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

  private fun SectionBlockBuilder.addBaseTranslationSection(
    baseTranslation: Translation,
    baseLanguage: Language,
  ) {
    markdownText(
      "*Base translation*: ${baseTranslation.text} *Language:* ${baseLanguage.name} ${baseLanguage.flagEmoji}",
    )
  }

  private fun shouldSkipModification(
    savedLanguageTags: Set<String>,
    translation: Translation,
    baseLanguage: Language,
    globalSubscription: Boolean,
  ): Boolean =
    !globalSubscription &&
      !savedLanguageTags.contains(
        translation.language.tag,
      ) && baseLanguage.id != translation.language.id

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
        val translation = foundKey.translations.find { it.language.tag == lang } ?: return null
        val color =
          determineColorByState(
            translation.state,
          )
        val bodyBlock =
          withBlocks {
            section {
              val langInfo = translation.language.let { "*Language:* ${it.name} ${it.flagEmoji}" }
              markdownText("*Current translate: * ${translation.text}\n $langInfo")
            }
            divider()

            val shouldSkip = shouldSkipInput(translation, slackConfig.languageTags)
            if (shouldSkip ||
              !hasPermissions(slackConfig.project.id, slackConfig.userAccount.id)
            ) {
              return@withBlocks
            }

            input {
              translateInput(keyId, translation.language)
            }
          }

        Attachment.builder()
          .color(color)
          .blocks(bodyBlock.toList())
          .fallback("New key added to Tolgee project")
          .build()
      } ?: return null

    return result
  }
}
