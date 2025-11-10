package io.tolgee.ee.component.slackIntegration

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.SlackApiException
import com.slack.api.methods.SlackApiTextResponse
import com.slack.api.methods.request.chat.ChatPostEphemeralRequest.ChatPostEphemeralRequestBuilder
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import com.slack.api.methods.request.chat.ChatUpdateRequest.ChatUpdateRequestBuilder
import com.slack.api.methods.response.chat.ChatPostEphemeralResponse
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.chat.ChatUpdateResponse
import com.slack.api.model.block.LayoutBlock
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Lazy
@Component
class SlackChannelMessagesOperations(
  private val tolgeeProperties: TolgeeProperties,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val slackClient: Slack,
) {
  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
  }

  fun sendEphemeralMessage(
    token: SlackToken,
    channelId: String,
    userId: String,
    blocks: List<LayoutBlock>,
    extra: RequestConfigurator<ChatPostEphemeralRequestBuilder>? = null,
  ): MessageSendResult<ChatPostEphemeralResponse> =
    trySend {
      getWorkspaceSlackClient(token).chatPostEphemeral {
        it.user(userId)
        it.channel(channelId)
        it.blocks(blocks)
        extra?.configure(it)
        it
      }
    }

  fun sendMessage(
    token: SlackToken,
    channelId: String,
    blocks: List<LayoutBlock>,
    extra: RequestConfigurator<ChatPostMessageRequestBuilder>? = null,
  ): MessageSendResult<ChatPostMessageResponse> =
    trySend {
      getWorkspaceSlackClient(token).chatPostMessage {
        it.channel(channelId)
        if (blocks.isNotEmpty()) {
          it.blocks(blocks)
        }
        extra?.configure(it)
        it
      }
    }

  fun updateMessage(
    token: SlackToken,
    channelId: String,
    messageTimestamp: String,
    update: RequestConfigurator<ChatUpdateRequestBuilder>? = null,
  ): MessageSendResult<ChatUpdateResponse> =
    trySend {
      getWorkspaceSlackClient(token).chatUpdate {
        it.channel(channelId)
        it.ts(messageTimestamp)
        update?.configure(it)
        it
      }
    }

  private fun getWorkspaceSlackClient(token: SlackToken): MethodsClient {
    val tokenString =
      when (token) {
        is SlackTeamId -> organizationSlackWorkspaceService.findBySlackTeamId(token.teamId)?.getSlackToken()
        is SlackWorkspaceToken -> token.token
      }

    return slackClient.methods(tokenString)!!
  }

  private fun <R : SlackApiTextResponse> trySend(method: () -> R): MessageSendResult<R> {
    try {
      val response = method.invoke()

      if (!response.isOk) {
        return ChatFailureResponseNotOk(response)
          .also { failure ->
            RuntimeException("Cannot send message in slack: ${failure.error}")
              .let { logger.error(it.message, it) }
          }
      }

      return ChatSuccess(response)
    } catch (e: SlackApiException) {
      return ChatFailureException<R>(e).also {
        logger.error("Cannot send message in slack: ${it.error}", it)
      }
    }
  }

  private fun OrganizationSlackWorkspace?.getSlackToken(): String {
    return this?.accessToken ?: tolgeeProperties.slack.token ?: throw SlackNotConfiguredException()
  }

  sealed interface MessageSendResult<R : SlackApiTextResponse>

  data class ChatSuccess<R : SlackApiTextResponse>(
    val response: R,
  ) : MessageSendResult<R>

  sealed interface ChatFailure<R : SlackApiTextResponse> : MessageSendResult<R> {
    val error: String
  }

  data class ChatFailureResponseNotOk<R : SlackApiTextResponse>(
    val response: R,
  ) : ChatFailure<R> {
    override val error: String
      get() = response.error
  }

  data class ChatFailureException<R : SlackApiTextResponse>(
    val exception: SlackApiException,
  ) : ChatFailure<R> {
    override val error: String
      get() = exception.error?.toString() ?: exception.message ?: "Unknown error"
  }

  sealed interface SlackToken

  data class SlackTeamId(
    val teamId: String,
  ) : SlackToken

  data class SlackWorkspaceToken(
    val token: String,
  ) : SlackToken
}
