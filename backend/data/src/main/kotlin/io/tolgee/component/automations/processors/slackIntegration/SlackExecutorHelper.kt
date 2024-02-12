package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.kotlin_extension.block.InputBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.model.kotlin_extension.view.blocks
import com.slack.api.model.view.View
import com.slack.api.model.view.Views.view
import com.slack.api.model.view.Views.viewTitle
import io.tolgee.constants.SlackEventActions
import io.tolgee.model.Language
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.key.KeyService

class SlackExecutorHelper(
  val slackConfig: SlackConfig,
  val data: SlackRequest,
  val keyService: KeyService
) {

  fun createKeyChangeMessage() = withBlocks {
    val activities = data.activityData ?: return@withBlocks

    section {
      markdownText("Project was modified :exclamation:")
    }

    val modifiedEntities = activities.modifiedEntities ?: return@withBlocks
    // Extracting Key and Translation Information
    modifiedEntities.forEach { (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        modifiedEntity.modifications?.forEach { (property, modification) ->
          val oldValue = modification.old?.toString() ?: "None"
          val newValue = modification.new?.toString() ?: "None"
          section {
            markdownText("*$entityType $property*\n  ~$oldValue~ -> $newValue")
          }
        }

        val keyId = modifiedEntity.entityId
        val key = keyService.get(keyId)
        key.translations.forEach { translation ->
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

  fun createKeyAddMessage() = withBlocks {
    val activities = data.activityData ?: return@withBlocks

    // Header Section
    section {
      markdownText("New Translation Key Added :exclamation:")
    }

    // Extracting Key and Translation Information
    val modifiedEntities = activities.modifiedEntities ?: return@withBlocks
    modifiedEntities.forEach { (entityType, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        when (entityType) {
          "Key" -> {
            val keyId = modifiedEntity.entityId
            val key = keyService.get(keyId)

            val keyName = key.name
            section {
              markdownText("*Key:* $keyName")
            }

            key.translations.forEach { translation ->
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

              input {
                translateInput(keyId, language)
              }
            }
          }
        }
      }
    }

    divider()

    // Author Section
    context {
      elements {
        val author = activities.author?.username ?: "Unknown Author"
        plainText("Author: $author")
      }
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

}
