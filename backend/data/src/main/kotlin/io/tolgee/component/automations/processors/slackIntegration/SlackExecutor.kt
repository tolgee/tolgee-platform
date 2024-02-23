package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.VisibilityOptions
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.PermissionService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class SlackExecutor(
  private val properties: TolgeeProperties,
  private val keyService: KeyService,
  private val permissionService: PermissionService,
) {

  private val slackToken = properties.slackProperties.slackToken
  private val slackClient: Slack = Slack.getInstance()
  private lateinit var slackExecutorHelper: SlackExecutorHelper

  fun sendMessageOnKeyChange() {
    val messageBlocks = slackExecutorHelper.createKeyChangeMessage()
    val config = slackExecutorHelper.slackConfig

    if (config.visibilityOptions == VisibilityOptions.ONLY_ME) {
      sendEphemeralMessage(config.channelId, config.slackId, messageBlocks)
    } else {
      sendRegularMessage(config.channelId, messageBlocks)
    }
  }

  fun sendMessageOnTranslationSet() {
    val config = slackExecutorHelper.slackConfig
    val (attachments, blocks) = slackExecutorHelper.createTranslationChangeMessage() ?: return

    if (config.visibilityOptions == VisibilityOptions.ONLY_ME) {
      sendEphemeralMessage(config.channelId, config.slackId, blocks, attachments)
    } else {
     sendRegularMessage(config.channelId, blocks, attachments)
    }
  }

  fun sendMessageOnKeyAdded() {
    val config = slackExecutorHelper.slackConfig
    val (attachments, blocks) = slackExecutorHelper.createKeyAddMessage() ?: return
    if (config.visibilityOptions == VisibilityOptions.ONLY_ME) {
      sendEphemeralMessage(config.channelId, config.slackId, blocks, attachments)
    } else {
      sendRegularMessage(config.channelId, blocks, attachments)
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
    slackClient.methods(slackToken).chatPostMessage {
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

  private fun sendEphemeralMessage(
    channelId: String,
    userId: String,
    blocks: List<LayoutBlock>,
    attachments: List<Attachment>? = null
    ) {
    slackClient.methods(slackToken).chatPostEphemeral { request ->
      request.channel(channelId)
        .user(userId)
        .blocks(blocks)
        .attachments(attachments)

    }
  }

  fun sendRegularMessage(
    channelId: String,
    blocks: List<LayoutBlock>,
    attachments: List<Attachment>? = null
    ) {
    slackClient.methods(slackToken).chatPostMessage { request ->
      request.channel(channelId)
        .blocks(blocks)
        .attachments(attachments)
    }

  }

  fun setHelper(
    slackConfig: SlackConfig,
    data: SlackRequest
  ) {
    slackExecutorHelper = SlackExecutorHelper(slackConfig, data, keyService, permissionService)
  }

}
