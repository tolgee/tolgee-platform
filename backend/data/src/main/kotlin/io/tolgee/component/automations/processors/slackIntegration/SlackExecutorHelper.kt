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
import io.tolgee.api.IModifiedEntityModel
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
  val permissionService: PermissionService
) {

  fun createKeyChangeMessage() = withBlocks {
    val activities = data.activityData ?: return@withBlocks

    section {
      markdownText("Project was modified :exclamation:")
    }
    val savedLanguageTag = slackConfig.languageTag
    val modifiedEntities = activities.modifiedEntities ?: return@withBlocks
    // Extracting Key and Translation Information
    modifiedEntities.forEach { (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        section {
          modificationInfo(modifiedEntity, entityType)
        }

        val keyId = modifiedEntity.entityId
        val key = keyService.get(keyId)

        key.translations.forEach translationLoop@{ translation ->
          //if user wasnt subscribed fot that language, skip it
          if (!savedLanguageTag.isNullOrBlank() && savedLanguageTag != translation.language.tag)
            return@translationLoop

          divider()
          section {
            val currentTranslate = translation.text ?: "None"
            markdownText("*Current translate:* $currentTranslate")
          }

          input {
            translateInput(keyId, translation.language)
          }
        }
      }
    }

    divider()

    context {
      elements {
        val author = activities.author?.username ?: "Unknown Author"
        plainText("Author: $author")
      }
    }
  }

  fun buildSuccessView(): View {
    return view { thisView -> thisView
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

  fun createKeyAddMessage(): Pair<List<Attachment>, List<LayoutBlock>>? {
    val activities = data.activityData ?: return null
    val attachments = mutableListOf<Attachment>()

    // Header Section as a block
    var blocksHeader: List<LayoutBlock> = listOf()

    // Extracting Key and Translation Information
    val modifiedEntities = activities.modifiedEntities ?: return null
    modifiedEntities.forEach modifiedEntities@{ (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        when (entityType) {
          "Key" -> {
            val keyId = modifiedEntity.entityId
            val key = keyService.get(keyId)

            val baseLanguage = slackConfig.project.baseLanguage ?: return@modifiedEntities

            blocksHeader = buildKeyInfoBlock(key)

            key.translations.forEach translations@{ translation ->
              val blocksBody = buildBodyForNewKey(translation, baseLanguage, keyId)
              val color = determineColorByState(translation.state)
              attachments.add(
                Attachment.builder()
                  .color(color)
                  .blocks(blocksBody.toList())
                  .fallback("New key added to Tolgee project")
                  .build()
              )
            }
          }
        }
      }
    }

    // Check if attachments list is empty
    if (attachments.isEmpty() || blocksHeader.isEmpty()) {
      return null
    }
    return Pair(attachments, blocksHeader)
  }

  private fun buildBodyForNewKey(
    translation: Translation,
    baseLanguage: Language,
    keyId: Long
  ) = withBlocks {
    if(shouldSkipModification(slackConfig.languageTag, translation, baseLanguage))
      return@withBlocks

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

    if(slackConfig.languageTag.isNullOrBlank() && !hasPermissions(slackConfig.project.id ,slackConfig.userAccount.id))
      return@withBlocks
    input {
      translateInput(keyId, language)
    }
  }

  private fun buildKeyInfoBlock(
    key: Key
  ) = withBlocks {
    section {
      markdownText("*Key:* ${key.name}")
    }

    section {
      markdownText("*Key namespace:* ${key.namespace ?: "None"}")
    }
  }

  fun createTranslationChangeMessage(): Pair<List<Attachment>, List<LayoutBlock>>? {
    val attachments = mutableListOf<Attachment>()
    var headerBlock: List<LayoutBlock> = emptyList()
    var footerBlock: List<LayoutBlock> = emptyList()
    val isBaseLanguageChangedEvent = slackConfig.onEvent == EventName.BASE_CHANGED

    data.activityData?.modifiedEntities?.forEach modifiedEntities@{ (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        modifiedEntity.modifications?.forEach modificationLoop@{ (property, modification) ->
          if (property != "text") return@modificationLoop

          val translationKey = modifiedEntity.entityId
          keyService.find(slackConfig.project.id, translationKey).getOrElse { return@modifiedEntities }.apply {
            val translation = translations.find { it.id == translationKey }
            val baseLanguage = slackConfig.project.baseLanguage ?: return@modifiedEntities
            val baseTranslation = translations.find { it.language.id == baseLanguage.id } ?: return@modifiedEntities

            val isBaseLanguage = baseLanguage.id == translation?.language?.id
            if ((isBaseLanguageChangedEvent && !isBaseLanguage) || (!isBaseLanguageChangedEvent && isBaseLanguage)) {
              return@modificationLoop
            }

            val savedLanguageTag = slackConfig.languageTag
            if (shouldSkipModification(savedLanguageTag, translation, baseLanguage)) return@modificationLoop

            val color = determineColorByState(translation?.state)
            val bodyBlock = buildBlocksForTranslation(translation, this, modification, entityType) // Функция для построения блоков
            headerBlock = buildKeyInfoBlock(this)
            footerBlock = withBlocks {
              section {
                addBaseTranslationSection(baseTranslation, baseLanguage)
              }
              divider()
            }

            attachments.add(Attachment.builder()
              .color(color)
              .blocks(bodyBlock)
              .fallback("New update in Tolgee project for ${this.name}")
              .build())
          }
        }
      }
    }

    return if (attachments.isEmpty()) {
      null
    } else {
      Pair(attachments, headerBlock + footerBlock)
    }
  }

  private fun buildBlocksForTranslation(
    translation: Translation?,
    key: Key,
    modification: PropertyModification,
    entityType: String
  ) = withBlocks {

    section {
      addModificationSection(entityType, entityType, modification, translation)
    }

    if (translation?.language == null ||
      slackConfig.languageTag.isNullOrBlank() ||
      !hasPermissions(slackConfig.project.id, slackConfig.userAccount.id))
      return@withBlocks

    input {
      translateInput(key.id, translation.language)
    }
  }

  private fun determineColorByState(state: TranslationState?): String {
    return when (state) {
      TranslationState.TRANSLATED -> "#36a64f"
      TranslationState.UNTRANSLATED -> "#a6363f"
      else -> "#cccccc"
    }
  }


  private fun InputBlockBuilder.translateInput(keyId: Long, language: Language) {
    dispatchAction(true)
    label(text = "Translate me ${language.flagEmoji}", emoji = true)
    plainTextInput {
      actionId(SlackEventActions.TRANSLATE_VALUE.name + "/$keyId" + "/${language.tag}")
      dispatchActionConfig {
        triggerActionsOn("on_enter_pressed")
      }
    }
  }

  private fun SectionBlockBuilder.modificationInfo(modifiedEntity: IModifiedEntityModel, entityType: String, language: Language? = null) {
    modifiedEntity.modifications?.forEach { (property, modification) ->
      if (property != "text" && property != "name")
        return@forEach
      val oldValue = modification.old?.toString() ?: "None"
      val newValue = modification.new?.toString() ?: "None"
      val langInfo = language?.let { "*Language:* ${it.name} ${it.flagEmoji}" } ?: ""
      markdownText("*$entityType $property* $langInfo \n  ~$oldValue~ -> $newValue")
    }
  }

  private fun SectionBlockBuilder.addKeyInformationSection(key: Key) {
    markdownText("*Key name*: ${key.name}")
    markdownText("*Key namespace*: ${key.namespace ?: "None"}")
  }

  private fun SectionBlockBuilder.addModificationSection(entityType: String, property: String, modification: PropertyModification, translation: Translation?) {
      val oldValue = modification.old?.toString() ?: "None"
      val newValue = modification.new?.toString() ?: "None"
      val langInfo = translation?.language?.let { "*Language:* ${it.name} ${it.flagEmoji}" } ?: ""
      markdownText("*$entityType $property* $langInfo \n  ~$oldValue~ -> $newValue")
  }

  private fun SectionBlockBuilder.addBaseTranslationSection(baseTranslation: Translation, baseLanguage: Language) {
      markdownText("*Base translation*: ${baseTranslation.text} *Language:* ${baseLanguage.name} ${baseLanguage.flagEmoji}")
  }

  private fun shouldSkipModification(savedLanguageTag: String?, translation: Translation?, baseLanguage: Language): Boolean =
    !savedLanguageTag.isNullOrBlank() && savedLanguageTag != translation?.language?.tag && baseLanguage.id != translation?.language?.id

  private fun hasPermissions(projectId: Long, userAccountId: Long): Boolean =
    permissionService.getProjectPermissionScopes(projectId, userAccountId)?.contains(Scope.TRANSLATIONS_EDIT) == true
}
