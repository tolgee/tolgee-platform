package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.component.FrontendUrlProvider
import org.springframework.stereotype.Component

@Component
class SlackUserLoginUrlProvider(
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  fun getUrl(
    slackChannelId: String,
    slackId: String,
    workspaceId: Long?,
  ) = "${frontendUrlProvider.url}/slack/login?slackId=$slackId&channelId=$slackChannelId&workspaceId=$workspaceId"
}
