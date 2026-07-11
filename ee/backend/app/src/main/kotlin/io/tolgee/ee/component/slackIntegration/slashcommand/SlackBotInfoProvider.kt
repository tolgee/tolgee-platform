package io.tolgee.ee.component.slackIntegration.slashcommand

import com.slack.api.Slack
import io.tolgee.dtos.request.slack.SlackCommandDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SlackBotInfoProvider(
  private val slackClient: Slack,
) {
  val logger: Logger by lazy {
    LoggerFactory.getLogger(javaClass)
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
      RuntimeException("Cannot get channel info in slack: ${response.error}")
        .let { logger.error(it.message, it) }
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
}
