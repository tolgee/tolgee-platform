package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.slack.SlackUserLoginDto
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.service.LanguageService
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
  private val slackUserLoginUrlProvider: SlackUserLoginUrlProvider,
  private val slackClient: Slack,
  private val languageService: LanguageService,
) : Logging {
  fun sendMessageOnTranslationSet(
    slackConfig: SlackConfig,
    request: SlackRequest,
  ) {
    val slackExecutorHelper = getHelper(slackConfig, request)
    val config = slackExecutorHelper.slackConfig
    val counts = slackExecutorHelper.data.activityData?.counts?.get("Translation") ?: 0
    if (counts >= 10) {
      val messageDto = slackExecutorHelper.createMessageIfTooManyTranslations(counts)
      sendRegularMessageWithSaving(messageDto, config)
      return
    }

    val messagesDto = slackExecutorHelper.createTranslationChangeMessage()

    messagesDto.forEach { message ->
      val savedMessage = findSavedMessageOrNull(message.keyId, config.id)

      if (savedMessage.isEmpty()) {
        sendRegularMessageWithSaving(message, config)
        return@forEach
      }

      savedMessage.forEach { savedMsg ->
        if (savedMsg.createdKeyBlocks) {
          message.blocks = emptyList()
        }
        processSavedMessage(savedMsg, message, config, slackExecutorHelper)
      }
    }
  }

  fun getSlackNickName(author: String?): String? {
    val response =
      slackClient.methods(tolgeeProperties.slack.token).usersLookupByEmail { req ->
        req.email(author)
      }

    return if (response.isOk) {
      response.user?.name
    } else {
      logger.info(response.error)
      null
    }
  }

  private fun processSavedMessage(
    savedMsg: SavedSlackMessage,
    message: SavedMessageDto,
    config: SlackConfig,
    slackExecutorHelper: SlackExecutorHelper,
  ) {
    val existingLanguages = savedMsg.langTags
    val newLanguages = message.langTag

    val languagesToAdd = existingLanguages - newLanguages
    if (languagesToAdd == existingLanguages) {
      return
    }

    val additionalAttachments: MutableList<Attachment> = mutableListOf()
    languagesToAdd.forEach { lang ->
      val attachment = slackExecutorHelper.createAttachmentForLanguage(lang, message.keyId)
      attachment?.let {
        additionalAttachments.add(it)
      }
    }

    val updatedAttachments = additionalAttachments + message.attachments
    val updatedLanguages = message.langTag + languagesToAdd
    val updatedMessageDto = message.copy(attachments = updatedAttachments, langTag = updatedLanguages)

    updateMessage(savedMsg, config, updatedMessageDto)
  }

  fun sortSoBaseLanguageFirst(attachments: MutableList<Attachment>): MutableList<Attachment> {
    val baseLanguageAttachmentIndex = attachments.indexOfFirst { it.blocks[0].toString().contains("(base)") }
    if (baseLanguageAttachmentIndex != -1) {
      val baseLanguageAttachment = attachments[baseLanguageAttachmentIndex]
      attachments.removeAt(baseLanguageAttachmentIndex)
      attachments.add(0, baseLanguageAttachment)
    }
    return attachments
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

  fun getLoginRedirectBlocks(
    slackChannelId: String,
    slackId: String,
    workspace: OrganizationSlackWorkspace?,
  ): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.not-connected"))
      }

      section {
        markdownText(i18n.translate("slack.common.message.connect-account-instruction"))
      }

      actions {
        button {
          text(i18n.translate("slack.common.text.button.connect"), emoji = true)
          value("connect_slack")
          url(slackUserLoginUrlProvider.getUrl(slackChannelId, slackId, workspace?.id))
          actionId("button_connect_slack")
          style("primary")
        }
      }
    }
  }

  fun sendUserLoginSuccessMessage(
    token: String,
    dto: SlackUserLoginDto,
  ) {
    slackClient.methods(token).chatPostMessage {
      it.channel(dto.slackChannelId)
        .blocks {
          section {
            markdownText(i18n.translate("slack.common.message.success_login"))
          }
          context {
            plainText(i18n.translate("slack.common.context.success_login"))
          }
        }
    }
  }

  fun sendBlocksMessage(
    teamId: String,
    channelId: String,
    blocks: List<LayoutBlock>,
  ) {
    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(teamId)
    slackClient.methods(workspace.getSlackToken()).chatPostMessage {
      it.channel(channelId)
        .blocks(blocks)
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
          .attachments(sortSoBaseLanguageFirst(messageDto.attachments.toMutableList()))
        if (messageDto.blocks.isNotEmpty()) {
          request.blocks(messageDto.blocks)
        }
        request
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
          .attachments(sortSoBaseLanguageFirst(messageDto.attachments.toMutableList()))
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
      getSlackNickName(data.activityData?.author?.name ?: ""),
    )
  }

  private fun findSavedMessageOrNull(
    keyId: Long,
    configId: Long,
  ) = savedSlackMessageService.find(keyId, configId)

  private fun saveMessage(
    messageDto: SavedMessageDto,
    ts: String,
    config: SlackConfig,
  ) {
    savedSlackMessageService.save(
      savedSlackMessage =
        SavedSlackMessage(
          messageTs = ts,
          slackConfig = config,
          keyId = messageDto.keyId,
          langTags = messageDto.langTag,
          messageDto.createdKeyBlocks,
        ),
    )
  }

  private fun updateLangTagsMessage(
    id: Long,
    langTags: Set<String>,
  ) {
    savedSlackMessageService.update(id, langTags)
  }

  fun getListOfSubscriptions(
    userId: String,
    channelId: String,
  ): List<LayoutBlock> {
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
              if (it.languageTag == null) {
                return@section
              }
              val language = languageService.getByTag(it.languageTag!!, config.project)
              val flagEmoji = language.flagEmoji

              val fullName = language.name
              markdownText(
                "*Subscribed Languages:*\n- $fullName $flagEmoji : on ${it.onEvent.name}",
              )
            }
          }
          divider()
        }
      }

    return blocks
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
      sendMessageOnKeyAdded(slackConfig, request)
    }
  }
}
