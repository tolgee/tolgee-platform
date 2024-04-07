package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.Attachment
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
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

  fun sendErrorMessage(
    errorMessage: Message,
    slackChannelId: String,
    slackId: String,
    slackNickName: String,
    workSpace: String,
    channelName: String,
    teamDomain: String,
  ) {
    val url =
      when (errorMessage) {
        Message.SLACK_NOT_LINKED_ORG ->
          getRedirectUrl(
            slackChannelId,
            workSpace,
            slackId,
            slackChannelId,
            channelName,
            teamDomain,
          )
        Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT -> getRedirectUrl(slackChannelId, slackId, slackNickName)
        else -> ""
      }
    val blocks = createErrorBlocks(errorMessage, url)

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

  fun connectOrganisationButton(
    slackChannelId: String,
    workSpace: String,
    userId: String,
    slackNickName: String,
    channelName: String,
    teamDomain: String,
  ) {
    slackClient.methods(slackToken).chatPostMessage {
      it.channel(slackChannelId)
        .blocks {
          section {
            markdownText(i18n.translate("org-not-linked-message"))
          }

          section {
            markdownText(i18n.translate("org-not-linked-instruction"))
          }

          actions {
            button {
              text(i18n.translate("connect-button-text"), emoji = true)
              value("connect_slack")
              url(getRedirectUrl(slackChannelId, workSpace, userId, slackChannelId, channelName, teamDomain))
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
            markdownText(i18n.translate("success_login_message"))
          }
          context {
            plainText(i18n.translate("success_login_context"))
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

  private fun getRedirectUrl(
    slackChannelId: String,
    workSpace: String,
    slackId: String,
    slackNickName: String,
    channelName: String,
    teamDomain: String,
  ) =
    "${tolgeeProperties.frontEndUrl}/slack/login?slackId=$slackId&channelId=$slackChannelId&nickName=$slackNickName&workSpace=$workSpace&channelName=$channelName&domainName=$teamDomain"

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

          Message.SLACK_NOT_LINKED_ORG ->
            i18n.translate("org-not-linked-message")

          else -> {
            i18n.translate("unknown-error-occurred")
          }
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

      Message.SLACK_NOT_LINKED_ORG -> {
        section {
          markdownText(i18n.translate("org-not-linked-instruction"))
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
          helpButton()
        }
      }

      Message.SLACK_NOT_SUBSCRIBED_YET -> {
        section {
          markdownText(i18n.translate("not-subscribed-solution"))
        }
        actions {
          helpButton()
        }
      }

      else -> {}
    }
  }

  private fun ActionsBlockBuilder.helpButton() {
    button {
      value("help_btn")
      text(i18n.translate("view-help-button-text"), emoji = true)
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
          config.preferences.forEach {
            section {
              markdownText("*Subscribed Languages:*\n- ${it.languageTag} :${it.languageTag}: on ${it.onEvent.name}")
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

  fun sendHelpMessage(channelId: String) {
    val response =
      slackClient.methods(slackToken).chatPostMessage { request ->
        request.channel(channelId)
          .blocks(helpBlocks())
      }

    if (!response.isOk) {
      logger.info(response.error)
    }
  }

  private fun helpBlocks() =
    withBlocks {
      section {
        markdownText(i18n.translate("help-intro"))
      }
      divider()
      section {
        markdownText(i18n.translate("help-subscribe"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-command"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-events"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-all-event"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-new-key-event"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-base-changed-event"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-translation-change-event"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-unsubscribe"))
      }

      section {
        markdownText(i18n.translate("help-unsubscribe-command"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-show-subscriptions"))
      }
      section {
        markdownText(i18n.translate("help-show-subscriptions-command"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-connect-tolgee"))
      }
      section {
        markdownText(i18n.translate("help-connect-tolgee-command"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-disconnect-tolgee"))
      }
      section {
        markdownText(i18n.translate("help-disconnect-tolgee-command"))
      }
    }

  fun sendMessageOnImport() {
    val config = slackExecutorHelper.slackConfig
    val messageDto = slackExecutorHelper.createImportMessage() ?: return

    sendRegularMessageWithSaving(messageDto, config)
  }
}
