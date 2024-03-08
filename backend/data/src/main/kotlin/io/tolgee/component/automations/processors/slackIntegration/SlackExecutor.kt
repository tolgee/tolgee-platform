package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.slackIntegration.SavedSlackMessageService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class SlackExecutor(
  properties: TolgeeProperties,
  private val keyService: KeyService,
  private val permissionService: PermissionService,
  private val savedSlackMessageService: SavedSlackMessageService,
) {

  private val slackToken = properties.slackProperties.slackToken
  private val slackClient: Slack = Slack.getInstance()
  private lateinit var slackExecutorHelper: SlackExecutorHelper

  fun sendMessageOnTranslationSet() {
    val config = slackExecutorHelper.slackConfig
    val messageDto = slackExecutorHelper.createTranslationChangeMessage() ?: return
    val savedMessage = findSavedMessageOrNull(messageDto.keyId, messageDto.langTag)

    if(savedMessage.isEmpty()) {
      sendRegularMessage(messageDto, config)
      return
    }

    savedMessage.forEach { savedMsg ->
      val existingLanguages = savedMsg.langTags
      val newLanguages = messageDto.langTag

      val languagesToAdd = existingLanguages - newLanguages
      if(languagesToAdd == existingLanguages)
        return@forEach

      val additionalAttachments = languagesToAdd.mapNotNull { lang ->
        slackExecutorHelper.createAttachmentForLanguage(lang, messageDto.keyId)
      }

      val updatedAttachments = messageDto.attachments + additionalAttachments
      val updatedMessageDto = messageDto.copy(attachments = updatedAttachments)

      updateMessage(savedMsg, config, updatedMessageDto)
    }

  }

  fun sendMessageOnKeyAdded() {
    val config = slackExecutorHelper.slackConfig
    val messageDto = slackExecutorHelper.createKeyAddMessage() ?: return

    sendRegularMessage(messageDto, config)
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

  private fun updateMessage(savedMessage: SavedSlackMessage, config: SlackConfig, messageDto: SavedMessageDto) {
    slackClient.methods(slackToken).chatUpdate { request ->
      request
        .channel(config.channelId)
        .ts(savedMessage.messageTs)
        .blocks(messageDto.blocks)
        .attachments(messageDto.attachments)
    }
  }

  private fun sendRegularMessage(
    messageDto: SavedMessageDto,
    config: SlackConfig
    ) {
    val response = slackClient.methods(slackToken).chatPostMessage { request ->
      request.channel(config.channelId)
        .blocks(messageDto.blocks)
        .attachments(messageDto.attachments)
    }
    if(response.isOk)
      saveMessage(messageDto, response.ts, config)
  }

  fun setHelper(
    slackConfig: SlackConfig,
    data: SlackRequest
  ) {
    slackExecutorHelper = SlackExecutorHelper(slackConfig, data, keyService, permissionService)
  }

  private fun findSavedMessageOrNull(keyId: Long, langTags: Set<String>) =
    savedSlackMessageService.find(keyId, langTags)

  private fun saveMessage(
    messageDto: SavedMessageDto,
    ts: String,
    config: SlackConfig,
  ) {
    savedSlackMessageService.create(
      savedSlackMessage = SavedSlackMessage(
        messageTs = ts,
        slackConfig = config,
        keyId = messageDto.keyId,
        langTags = messageDto.langTag
      )
    )
  }
}
