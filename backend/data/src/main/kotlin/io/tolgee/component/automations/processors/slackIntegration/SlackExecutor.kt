package io.tolgee.component.automations.processors.slackIntegration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.dtos.request.slack.SlackConnectionDto
import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.service.slackIntegration.SlackConfigService
import io.tolgee.service.slackIntegration.SlackUserConnectionService
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
  private val slackUserConnectionService: SlackUserConnectionService,
  private val slackConfigService: SlackConfigService,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val frontendUrlProvider: FrontendUrlProvider,
  private val objectMapper: ObjectMapper,
) : Logging {
  private val slackClient: Slack = Slack.getInstance()

  fun sendMessageOnTranslationSet(
    slackConfig: SlackConfig,
    request: SlackRequest,
  ) {
    val slackExecutorHelper = getHelper(slackConfig, request)
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

  fun sendMessageOnKeyAdded(
    slackConfig: SlackConfig,
    request: SlackRequest,
  ) {
    val slackExecutorHelper = getHelper(slackConfig, request)
    val config = slackExecutorHelper.slackConfig
    val messagesDto = slackExecutorHelper.createKeyAddMessage()

    messagesDto.forEach { message ->
      sendRegularMessageWithSaving(message, config)
    }
  }

  fun getErrorMessage(
    errorMessage: Message,
    dto: SlackCommandDto,
    workspace: OrganizationSlackWorkspace?,
  ): SlackMessageDto {
    val url =
      when (errorMessage) {
        Message.SLACK_NOT_CONNECTED_TO_YOUR_ACCOUNT -> getRedirectUrl(dto.channel_id, dto.user_id, workspace?.id)
        else -> ""
      }

    return createErrorBlocks(errorMessage, url).asSlackMessageDto
  }

  val List<LayoutBlock>.asSlackMessageDto: SlackMessageDto
    get() {
      val withoutNulls =
        jacksonObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          .writeValueAsString(this)

      val resultBlocks = objectMapper.readValue<List<Any>>(withoutNulls)

      return SlackMessageDto(blocks = resultBlocks)
    }

  fun sendRedirectUrl(
    slackChannelId: String,
    slackId: String,
    workspace: OrganizationSlackWorkspace?,
  ) {
    slackClient.methods(workspace?.getSlackToken()).chatPostMessage {
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
              url(getRedirectUrl(slackChannelId, slackId, workspace?.id))
              actionId("button_connect_slack")
              style("primary")
            }
          }
        }
    }
  }

  fun sendSuccessMessage(dto: SlackConnectionDto) {
    val workspace = organizationSlackWorkspaceService.get(dto.workspaceId)
    slackClient.methods(workspace.getSlackToken()).chatPostMessage {
      it.channel(dto.channelId)
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
      slackClient.methods(config.organizationSlackWorkspace.getSlackToken()).chatUpdate { request ->
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
      slackClient.methods(config.organizationSlackWorkspace.getSlackToken()).chatPostMessage { request ->
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

  fun getHelper(
    slackConfig: SlackConfig,
    data: SlackRequest,
  ): SlackExecutorHelper {
    return SlackExecutorHelper(
      slackConfig,
      data,
      keyService,
      permissionService,
      slackUserConnectionService,
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
    workspaceId: Long?,
  ) = "${frontendUrlProvider.url}/slack/login?slackId=$slackId&channelId=$slackChannelId&workspaceId=$workspaceId"

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

  fun getListOfSubscriptions(
    userId: String,
    channelId: String,
  ): SlackMessageDto {
    val configList = slackConfigService.getAllByChannelId(channelId)

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

            section {
              markdownText("Events: ${config.onEvent}")
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

    return blocks.asSlackMessageDto
  }

  fun sendHelpMessage(
    channelId: String,
    teamId: String,
  ): SlackMessageDto {
    return helpBlocks.asSlackMessageDto
  }

  private val helpBlocks
    get() =
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

  private fun OrganizationSlackWorkspace?.getSlackToken(): String {
    return this?.accessToken ?: tolgeeProperties.slack.token ?: throw SlackNotConfiguredException()
  }

  fun sendMessageOnImport(
    slackConfig: SlackConfig,
    request: SlackRequest,
  ) {
    val slackExecutorHelper = getHelper(slackConfig, request)
    val config = slackExecutorHelper.slackConfig
    val counts = slackExecutorHelper.data.activityData?.counts?.get("Key") ?: return
    if (counts >= 10) {
      val messageDto = slackExecutorHelper.createImportMessage() ?: return
      sendRegularMessageWithSaving(messageDto, config)
    } else {
      sendMessageOnKeyAdded()
    }
  }

  fun getWorkspaceNotFoundError(): SlackMessageDto {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack-workspace-not-connected-to-any-organization"))
      }
    }.asSlackMessageDto
  }
}
