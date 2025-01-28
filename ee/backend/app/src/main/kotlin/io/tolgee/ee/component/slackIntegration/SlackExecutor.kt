package io.tolgee.ee.component.slackIntegration

import com.slack.api.Slack
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.dtos.request.slack.SlackUserLoginDto
import io.tolgee.ee.service.slackIntegration.*
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.PermissionService
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
  private val slackMessageInfoService: SlackMessageInfoService,
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
    val savedMessages = savedSlackMessageService.findAll(messagesDto, config.id)

    messagesDto.forEach { message ->

      if (savedMessages.isEmpty()) {
        sendRegularMessageWithSaving(message, config)
        return@forEach
      }

      savedMessages.forEach { savedMessage ->
        processSavedMessage(savedMessage, message, config, slackExecutorHelper)
      }

      if (shouldSendNewMessage(message, config)) {
        sendRegularMessageWithSaving(message, config)
      }
    }
  }

  fun getSlackNickName(authorId: Long): String? {
    val slackId = slackUserConnectionService.findByUserAccountId(authorId)?.slackUserId ?: return null
    return "<@$slackId>"
  }

  private fun processSavedMessage(
    savedMsg: SavedSlackMessage,
    message: SavedMessageDto,
    config: SlackConfig,
    slackExecutorHelper: SlackExecutorHelper,
  ) {
    val existingLanguages = savedMsg.info.map { it.languageTag }
    val newLanguages = message.langTag

    val languagesToAdd = existingLanguages - newLanguages
    if (languagesToAdd == existingLanguages) {
      return
    }

    val additionalAttachments: MutableList<Attachment> = mutableListOf()
    languagesToAdd.forEach { lang ->

      val authorContext = savedMsg.info.find { it.languageTag == lang }?.authorContext

      val attachment = slackExecutorHelper.createAttachmentForLanguage(lang, message.keyId, authorContext)
      attachment?.let {
        additionalAttachments.add(it)
      }
    }
    val updatedAttachments = additionalAttachments + message.attachments
    val updatedLanguages = message.langTag + languagesToAdd
    val authorContextMap = savedMsg.info.associate { it.languageTag to it.authorContext }
    val updatedMessageDto =
      message.copy(
        attachments = addAuthorContextToAttachments(updatedAttachments.toMutableList(), authorContextMap, config),
        langTag = updatedLanguages,
      )

    if (savedMsg.createdKeyBlocks) {
      updatedMessageDto.blocks = emptyList()
    }
    updateMessage(savedMsg, config, updatedMessageDto)
  }

  private fun addAuthorContextToAttachments(
    additionalAttachments: MutableList<Attachment>,
    authorContextMap: Map<String, String>,
    config: SlackConfig,
  ): List<Attachment> {
    authorContextMap.forEach { (langTag, author) ->
      val fullLanguageName = languageService.getByTag(langTag, config.project).name

      additionalAttachments.forEach attachments@{ attachment ->
        if (attachment.blocks[0].toString().contains(fullLanguageName) && attachment.blocks.size != 3) {
          if (author.isEmpty()) {
            return@attachments
          }
          attachment.blocks = attachment.blocks + getAuthorBlocks(author)
        }
      }
    }

    return additionalAttachments
  }

  private fun getAuthorBlocks(authorContext: String): List<LayoutBlock> {
    return withBlocks {
      context {
        markdownText(authorContext)
      }
    }
  }

  private fun shouldSendNewMessage(
    message: SavedMessageDto,
    config: SlackConfig,
  ): Boolean {
    val languages = message.langTag
    val subscribedLanguages = config.preferences.mapNotNull { it.languageTag }

    return languages.any { it in subscribedLanguages } && languages.size <= 2
  }

  fun sortAttachments(attachments: MutableList<Attachment>): MutableList<Attachment> {
    fun getLanguageName(attachment: Attachment): String {
      val textBlock = attachment.blocks[0].toString()
      // Assuming the language name is surrounded by asterisks (e.g., 🇸🇬 *Chinese*)
      return textBlock.substringAfter('*').substringBefore('*').trim()
    }

    val baseLanguageAttachmentIndex =
      attachments.indexOfFirst {
        it.blocks[0].toString().contains("(base)")
      }
    val baseLanguageAttachment =
      if (baseLanguageAttachmentIndex != -1) {
        attachments.removeAt(baseLanguageAttachmentIndex)
      } else {
        null
      }

    val buttonAttachment = attachments.removeAt(attachments.size - 1)

    attachments.sortBy { getLanguageName(it) }

    baseLanguageAttachment?.let {
      attachments.add(0, it)
    }

    attachments.add(buttonAttachment)

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
    slackTeamId: String,
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
          url(slackUserLoginUrlProvider.getUrl(slackChannelId, slackId, workspace?.id, slackTeamId))
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
    slackClient.methods(token).chatPostEphemeral {
      it.user(dto.slackUserId)
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
          .ts(savedMessage.messageTimestamp)
          .attachments(sortAttachments(messageDto.attachments.toMutableList()))
        if (messageDto.blocks.isNotEmpty()) {
          request.blocks(messageDto.blocks)
        }
        request
      }

    if (response.isOk) {
      updateLangTagsMessage(savedMessage.id, messageDto.langTag, messageDto.authorContext)
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
          .attachments(sortAttachments(messageDto.attachments.toMutableList()))
      }
    if (response.isOk) {
      saveMessage(messageDto, response.ts, config)
    } else {
      logger.info(response.error)
    }
  }

  fun isBotInChannel(
    payload: SlackCommandDto,
    token: String,
  ): Boolean {
    val response =
      slackClient.methods(token).conversationsInfo {
        it.channel(payload.channel_id)
      }

    if (!response.isOk) {
      logger.info(response.error)
      return false
    }

    if (!response.channel.isPrivate) {
      return true
    }

    if (response.channel.isIm) {
      return true
    }

    return response.channel.isMember
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
      i18n,
      tolgeeProperties,
      getSlackNickName(data.activityData?.author?.id ?: 0L),
      slackMessageInfoService,
    )
  }

  private fun saveMessage(
    messageDto: SavedMessageDto,
    ts: String,
    config: SlackConfig,
  ) {
    savedSlackMessageService.save(
      savedSlackMessage =
        SavedSlackMessage(
          messageTimestamp = ts,
          slackConfig = config,
          keyId = messageDto.keyId,
          languageTags = messageDto.langTag,
          messageDto.createdKeyBlocks,
        ),
      messageDto.authorContext,
      messageDto.langTag,
    )
  }

  private fun updateLangTagsMessage(
    id: Long,
    langTags: Set<String>,
    authorContextMap: Map<String, String>,
  ) {
    savedSlackMessageService.update(id, langTags, authorContextMap)
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
            markdownText("*Project Name:* ${config.project.name} (id: ${config.project.id})")
          }
          if (config.isGlobalSubscription) {
            section {
              markdownText("*Global Subscription:* Yes")
            }

            val events = config.events.joinToString(", ") { "$it" }

            val allEventMeaning =
              if (config.events.contains(SlackEventType.ALL)) {
                getEventAllMeaning()
              } else {
                ""
              }

            section {
              markdownText("*Events:* `$events` $allEventMeaning")
            }

            context {
              markdownText(
                i18n.translate("slack.common.message.list-subscriptions-global-subscription-meaning"),
              )
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
              val events = it.events.joinToString(", ") { "$it" }
              val allEventMeaning =
                if (it.events.contains(SlackEventType.ALL)) {
                  getEventAllMeaning()
                } else {
                  ""
                }
              markdownText(
                "*Subscribed Languages:*\n- $fullName $flagEmoji : on `$events` $allEventMeaning",
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

  private fun getEventAllMeaning() =
    "(" +
      i18n.translate(
        "slack.common.message.list-subscriptions-all-events",
      ) + ")"

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

  fun getSuccessfullySubscribedBlocks(config: SlackConfig): List<LayoutBlock> =
    withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.subscribed-successfully").format(config.project.name))
      }
      val subscriptionInfo =
        buildString {
          if (config.isGlobalSubscription) {
            val events = config.events.joinToString(", ") { "$it" }
            append(
              i18n.translate(
                "slack.common.message.subscribed-successfully-global-subscription",
              ).format(events),
            )
          }

          val languageInfo =
            config.preferences.mapNotNull { pref ->
              pref.languageTag?.let { tag ->
                val language = languageService.getByTag(tag, config.project)
                val flagEmoji = language.flagEmoji
                val fullName = language.name
                val events = pref.events.joinToString(", ") { "$it" }
                " - $fullName $flagEmoji : on `$events`"
              }
            }.joinToString("\n")

          if (languageInfo.isNotEmpty()) {
            if (config.isGlobalSubscription) {
              append("\n\n").append(i18n.translate("slack.common.message.also-subscribed-to"))
            } else {
              append(
                "\n\n",
              ).append(i18n.translate("slack.common.message.subscribed-successfully-not-global-subscription"))
            }
            append("\n").append(languageInfo)
          }
        }

      section {
        markdownText(subscriptionInfo)
      }
    }
}
