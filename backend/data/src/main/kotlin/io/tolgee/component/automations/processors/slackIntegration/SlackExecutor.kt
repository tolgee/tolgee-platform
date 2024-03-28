package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.Attachment
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackSubscriptionService
import io.tolgee.util.I18n
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class SlackExecutor(
  private val tolgeeProperties: TolgeeProperties,
  private val keyService: KeyService,
  private val permissionService: PermissionService,
  private val savedSlackMessageService: SavedSlackMessageService,
  private val i18n: I18n,
  private val slackSubscriptionService: SlackSubscriptionService,
  private val slackConfigService: SlackConfigService,
) : Logging {
  private val slackToken = tolgeeProperties.slack.token
  private val slackClient: Slack = Slack.getInstance()
  private lateinit var slackExecutorHelper: SlackExecutorHelper

  fun sendMessageOnTranslationSet() {
    val config = slackExecutorHelper.slackConfig
    val messageDto = slackExecutorHelper.createTranslationChangeMessage() ?: return
    val savedMessage = findSavedMessageOrNull(messageDto.keyId, messageDto.langTag, config.id)

    if (savedMessage.isEmpty()) {
      sendRegularMessageWithSaving(messageDto, config)
      return
    }

    savedMessage.forEach { savedMsg ->
      val existingLanguages = savedMsg.langTags
      val newLanguages = messageDto.langTag

      val languagesToAdd = existingLanguages - newLanguages
      if (languagesToAdd == existingLanguages) {
        return@forEach
      }

      val additionalAttachments: MutableList<Attachment> = mutableListOf()

      languagesToAdd.forEach { lang ->
        val attachment = slackExecutorHelper.createAttachmentForLanguage(lang, messageDto.keyId)
        attachment?.let {
          additionalAttachments.add(it)
        }
      }

      val updatedAttachments = additionalAttachments + messageDto.attachments
      val updatedLanguages = messageDto.langTag + languagesToAdd
      val updatedMessageDto = messageDto.copy(attachments = updatedAttachments, langTag = updatedLanguages)

      updateMessage(savedMsg, config, updatedMessageDto)
    }
  }

  fun sendMessageOnKeyAdded() {
    val config = slackExecutorHelper.slackConfig
    val messageDto = slackExecutorHelper.createKeyAddMessage() ?: return

    sendRegularMessageWithSaving(messageDto, config)
  }

  fun sendSuccessModal(triggerId: String) {
    slackClient.methods(slackToken).viewsOpen {
      it.triggerId(triggerId)
        .view(slackExecutorHelper.buildSuccessView())
    }
  }

  fun sendErrorMessage(
    errorMessage: Message,
    slackChannelId: String,
    slackId: String,
    slackNickName: String,
  ) {
    val blocks = createErrorBlocks(errorMessage, getRedirectUrl(slackChannelId, slackId, slackNickName))

    slackClient.methods(slackToken).chatPostMessage { request ->
      request.channel(slackChannelId)
        .blocks(blocks)
    }
  }

  fun sendRedirectUrl(
    slackChannelId: String,
    slackId: String,
    slackNickName: String,
  ) {
    slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackChannelId)
        .blocks {
          section {
            markdownText(i18n.translate("slack-not-connected-message"))
          }

          section {
            markdownText(i18n.translate("connect-account-instruction"))
          }

          actions {
            button {
              text(i18n.translate("connect-button-text"), emoji = true)
              value("connect_slack")
              url(getRedirectUrl(slackChannelId, slackId, slackNickName))
              actionId("button_connect_slack")
              style("primary")
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

  private fun updateMessage(
    savedMessage: SavedSlackMessage,
    config: SlackConfig,
    messageDto: SavedMessageDto,
  ) {
    val response =
      slackClient.methods(slackToken).chatUpdate { request ->
        request
          .channel(config.channelId)
          .ts(savedMessage.messageTs)
          .blocks(messageDto.blocks)
          .attachments(messageDto.attachments)
      }

    if (response.isOk) {
      updateLangTagsMessage(savedMessage.id, messageDto.langTag)
    } else {
      logger.info(response.error)
    }
  }

  private fun sendRegularMessageWithSaving(
    messageDto: SavedMessageDto,
    config: SlackConfig,
  ) {
    val response =
      slackClient.methods(slackToken).chatPostMessage { request ->
        request.channel(config.channelId)
          .blocks(messageDto.blocks)
          .attachments(messageDto.attachments)
      }
    if (response.isOk) {
      saveMessage(messageDto, response.ts, config)
    } else {
      logger.info(response.error)
    }
  }

  fun setHelper(
    slackConfig: SlackConfig,
    data: SlackRequest,
  ) {
    slackExecutorHelper =
      SlackExecutorHelper(
        slackConfig,
        data,
        keyService,
        permissionService,
        slackSubscriptionService,
        i18n,
        tolgeeProperties,
      )
  }

  private fun findSavedMessageOrNull(
    keyId: Long,
    langTags: Set<String>,
    configId: Long,
  ) = savedSlackMessageService.find(keyId, langTags, configId)

  private fun saveMessage(
    messageDto: SavedMessageDto,
    ts: String,
    config: SlackConfig,
  ) {
    savedSlackMessageService.create(
      savedSlackMessage =
        SavedSlackMessage(
          messageTs = ts,
          slackConfig = config,
          keyId = messageDto.keyId,
          langTags = messageDto.langTag,
        ),
    )
  }

  private fun updateLangTagsMessage(
    id: Long,
    langTags: Set<String>,
  ) {
    savedSlackMessageService.update(id, langTags)
  }

  private fun getRedirectUrl(
    slackChannelId: String,
    slackId: String,
    slackNickName: String,
  ) = "${tolgeeProperties.frontEndUrl}/slack/login?slackId=$slackId&channelId=$slackChannelId&nickName=$slackNickName"

  fun createErrorBlocks(
    errorMessageType: Message,
    redirectUrl: String,
  ) = withBlocks {
    section {
      markdownText(
        when (errorMessageType) {
          Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT ->
            i18n.translate("slack-not-connected-message")

          Message.SLACK_INVALID_COMMAND ->
            i18n.translate("command-not-recognized")

          Message.SLACK_NOT_SUBSCRIBED_YET ->
            i18n.translate("not-subscribed-yet-message")

          else ->
            i18n.translate("unknown-error-occurred")
        },
      )
    }

    when (errorMessageType) {
      Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT -> {
        section {
          markdownText(i18n.translate("connect-account-instruction"))
        }
        actions {
          button {
            text(i18n.translate("connect-button-text"), emoji = true)
            url(redirectUrl)
            style("primary")
          }
        }
      }

      Message.SLACK_INVALID_COMMAND -> {
        section {
          markdownText(i18n.translate("check-command-solutions"))
        }
        actions {
          button {
            text(i18n.translate("view-help-button-text"), emoji = true)
          }
        }
      }

      Message.SLACK_NOT_SUBSCRIBED_YET -> {
        section {
          markdownText(i18n.translate("not-subscribed-solution"))
        }
        actions {
          button {
            text(i18n.translate("view-help-button-text"), emoji = true)
          }
        }
      }

      else -> {}
    }
  }

  fun sendListOfSubscriptions(
    userId: String,
    channelId: String,
  ) {
    val configList = slackConfigService.get(userId, channelId)
    val blocks =
      withBlocks {
        header {
          text("Subscription Details", emoji = true)
        }
        divider()

        configList.forEach { config ->
          section {
            markdownText("*Project Name:* ${config.project.name}\n*Project ID:* ${config.project.id}")
          }
          if (config.isGlobalSubscription) {
            section {
              markdownText("*Global Subscription:* Yes")
            }
          }
          if (config.languageTags.isNotEmpty()) {
            val subscribedLanguages = config.languageTags.joinToString(separator = "\n") { "- $it :$it: " }
            section {
              markdownText("*Subscribed Languages:*\n$subscribedLanguages")
            }
          }
          divider()
        }
      }

    val response =
      slackClient.methods(slackToken).chatPostMessage { request ->
        request.channel(channelId)
          .blocks(blocks)
      }

    if (!response.isOk) {
      logger.info(response.error)
    }
  }
}
