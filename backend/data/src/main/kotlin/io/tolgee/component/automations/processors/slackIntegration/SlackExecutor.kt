package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
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
  private val slackToken = properties.slackProperties.slackToken
  private val slackClient: Slack = Slack.getInstance()
  private lateinit var slackExecutorHelper: SlackExecutorHelper

  fun sendMessageOnKeyChange() {
    val activities = slackExecutorHelper.data.activityData ?: return

    val response = slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackExecutorHelper.slackConfig.channelId)
        .blocks (slackExecutorHelper.createKeyChangeMessage(activities))
    }

    //todo error handling
    if (response.isOk) {
      println("Sent to ${slackExecutorHelper.slackConfig.channelId}")
    } else {
      println("Error: ${response.error}")
    }
  }

  fun sendMessageOnKeyAdded() {
    val activities = slackExecutorHelper.data.activityData ?: return

    slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackExecutorHelper.slackConfig.channelId)
        .blocks(slackExecutorHelper.createKeyAddMessage(activities))
    }
  }

  fun sendErrorMessage(errorMessage: Message, slackChannelId: String) {
    val response = slackClient.methods(slackToken).chatPostMessage {
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

  fun setHelper(
    slackConfig: SlackConfig,
    data: SlackRequest
  ) {
    slackExecutorHelper = SlackExecutorHelper(slackConfig, data)
  }


}
