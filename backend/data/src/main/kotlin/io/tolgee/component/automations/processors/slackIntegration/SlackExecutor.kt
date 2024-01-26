package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import io.tolgee.model.slackIntegration.SlackConfig
import org.springframework.stereotype.Component

@Component
class SlackExecutor {

  fun sendMessage(
    slackConfig: SlackConfig,
    data: SlackRequest
  ) {
    val activities = data.activityData ?: return

    val response = slackClient.methods(SLACK_TOKEN).chatPostMessage {
      it.channel(slackConfig.channelId)
        .blocks {
          section {
            markdownText("*Something happen with project*")
          }
          divider()
          section {
            val author = activities.author?.username ?: return@section
            plainText(author + "") //need to refactor this concat
          }

          section {
            val modifiedEntities = activities.modifiedEntities ?: return@section
            val stringBuilder = StringBuilder()

            modifiedEntities.forEach { (entityId, modifiedEntityList) ->
              modifiedEntityList.forEach { modifiedEntity ->
                stringBuilder.append("Modified: $entityId\n")
                modifiedEntity.modifications?.forEach { (property, modification) ->
                  val oldValue = modification.old?.toString() ?: "None"
                  val newValue = modification.new?.toString() ?: "None"
                  stringBuilder.append("Property: $property\n, Old Value: $oldValue, New Value: $newValue\n")
                }
              }
            }

           // stringBuilder.append(data.activityData?.toString() ?: "")
            plainText(stringBuilder.toString())
          }


        }

    }

    if (response.isOk) {
      println("Sent to ${slackConfig.channelId}")
    } else {
      println("Error: ${response.error}")
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

          if(errorMessage == Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT) {
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
  companion object {
    val SLACK_TOKEN = "xoxb-6460981223175-6480877302916-kVJzxYw4v4AdNjX4x3wWck32" //TODO refactor
    val slackClient = Slack.getInstance()
  }
}
