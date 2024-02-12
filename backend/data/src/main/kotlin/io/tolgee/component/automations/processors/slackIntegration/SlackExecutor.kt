package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.key.KeyService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class SlackExecutor(
  val properties: TolgeeProperties,
  val keyService: KeyService
) {

  private val slackToken = properties.slackProperties.slackToken
  private val slackClient: Slack = Slack.getInstance()
  private lateinit var slackExecutorHelper: SlackExecutorHelper

  fun sendMessageOnKeyChange() {
    val response = slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackExecutorHelper.slackConfig.channelId)
        .blocks (slackExecutorHelper.createKeyChangeMessage())
    }

  }

  fun sendMessageOnKeyAdded() {
    slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackExecutorHelper.slackConfig.channelId)
        .blocks(slackExecutorHelper.createKeyAddMessage())
    }
  }

  fun sendSuccessModal(triggerId: String) {
    slackClient.methods(slackToken).viewsOpen {
      it.triggerId(triggerId)
        .view(slackExecutorHelper.buildSuccessView())
    }
  }

  fun sendErrorMessage(errorMessage: Message, slackChannelId: String) {
    slackClient.methods(slackToken).chatPostMessage {
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
  }

  fun sendRedirectUrl(slackChannelId: String, slackId: String) {
    val response = slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackChannelId)
        .blocks {
          section {
            markdownText(" :no_entry: You need to connect Slack to Tolgee first. ")
          }
          actions {
            button {
              val redirectUrl = "http://localhost:3000/slack/login?slackId=$slackId&channelId=$slackChannelId"

              text("Connect Slack to Tolgee", emoji = true)
              value("connect_slack")
              url(redirectUrl)
              actionId("button_connect_slack")
            }
          }
        }
    }

  }

  fun sendSuccessMessage(slackChannelId: String) {
    slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackChannelId)
        .blocks {
          section {
            markdownText("Success! :tada: The operation was completed successfully.")
          }
          context {
            plainText("Now you can use other commands")
          }
        }
    }
  }

  fun setHelper(
    slackConfig: SlackConfig,
    data: SlackRequest
  ) {
    slackExecutorHelper = SlackExecutorHelper(slackConfig, data, keyService)
  }

}
