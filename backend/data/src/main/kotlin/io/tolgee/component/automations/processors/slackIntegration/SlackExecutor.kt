package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.api.IProjectActivityModel
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.model.slackIntegration.SlackConfig
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class SlackExecutor(
  val properties: TolgeeProperties
) {
  val SLACK_TOKEN = properties.slackProperties.slackToken
  val slackClient: Slack = Slack.getInstance()

  fun sendMessageOnKeyChange(
    slackConfig: SlackConfig,
    data: SlackRequest
  ) {
    val activities = data.activityData ?: return

    val response = slackClient.methods(SLACK_TOKEN).chatPostMessage {
      it.channel(slackConfig.channelId)
        .blocks (
          createKeyChangeMessage(activities)
        )
    }

    //todo error handling
    if (response.isOk) {
      println("Sent to ${slackConfig.channelId}")
    } else {
      println("Error: ${response.error}")
    }
  }

  fun sendMessageOnKeyAdded(
    slackConfig: SlackConfig,
    data: SlackRequest
  ) {
    val activities = data.activityData ?: return

    slackClient.methods(SLACK_TOKEN).chatPostMessage {
      it.channel(slackConfig.channelId)
        .blocks(
          createKetAddMessage(activities)
        )
    }
  }

  fun sendErrorMessage(errorMessage: Message, slackChannelId: String) {
    val response = slackClient.methods(SLACK_TOKEN).chatPostMessage {
      it.channel(slackChannelId)
        .blocks {
          section {
            val emojiUnicode = "x"
            markdownText(":$emojiUnicode: ${errorMessage.code}")
          }

          if (errorMessage == Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT) {
            context {
              elements {
                val suggestion = "Try to use /login"
                plainText(suggestion)
              }
            }
          }
        }
    }

    if (response.isOk) {
      println("Sent to ${slackChannelId}")
    } else {
      println("Error: ${response.error}")
    }
  }

  private fun createKeyChangeMessage(activities: IProjectActivityModel) = withBlocks {
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

  private fun createKetAddMessage(activities: IProjectActivityModel) = withBlocks {
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
