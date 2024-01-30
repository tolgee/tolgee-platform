package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.api.IProjectActivityModel
import io.tolgee.model.slackIntegration.SlackConfig

class SlackExecutorHelper(
  val slackConfig: SlackConfig,
  val data: SlackRequest
) {

  fun createKeyChangeMessage(activities: IProjectActivityModel) = withBlocks {
    section {
      markdownText("Project was modified :exclamation:")
    }

    val modifiedEntities = activities.modifiedEntities ?: return@withBlocks

    modifiedEntities.forEach { (entityId, modifiedEntityList) ->
      modifiedEntityList.forEach { modifiedEntity ->
        modifiedEntity.modifications?.forEach { (property, modification) ->
          val oldValue = modification.old?.toString() ?: "None"
          val newValue = modification.new?.toString() ?: "None"
          section {
            markdownText("*$entityId $property*\n  ~$oldValue~ -> $newValue")
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

  fun createKetAddMessage(activities: IProjectActivityModel) = withBlocks {
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
            val keyName = modifiedEntity.modifications?.get("name")?.new?.toString() ?: "Unknown Key"
            section {
              markdownText("*Key:* $keyName")
            }
          }

          "Translation" -> {
            val stateModification = modifiedEntity.modifications?.get("state")?.new?.toString() ?: "UNTRANSLATED"
            val languageRelation = modifiedEntity.relations?.get("language")?.data as? Map<String, Any>
            val languageName = languageRelation?.get("name")?.toString() ?: "Unknown Language"
            val flagEmoji = languageRelation?.get("flagEmoji")?.toString() ?: ":world_map:"
            section {
              markdownText("*Translation State:* $stateModification\n*Language:* $languageName $flagEmoji")
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

}
