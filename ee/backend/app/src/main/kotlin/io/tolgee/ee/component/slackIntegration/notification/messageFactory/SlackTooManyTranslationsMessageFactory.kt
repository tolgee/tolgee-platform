package io.tolgee.ee.component.slackIntegration.notification.messageFactory

import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageContext
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageUrlProvider
import io.tolgee.ee.component.slackIntegration.notification.SlackNotificationBlocksProvider
import org.springframework.stereotype.Component

@Component
class SlackTooManyTranslationsMessageFactory(
  private val slackMessageUrlProvider: SlackMessageUrlProvider,
  private val blocksProvider: SlackNotificationBlocksProvider,
) {
  fun createMessageIfTooManyTranslations(context: SlackMessageContext): SlackMessageDto {
    return SlackMessageDto(
      blocks = blocksProvider.getBlocksTooManyTranslations(context, context.modifiedTranslationsCount),
      attachments = listOf(blocksProvider.getRedirectButtonAttachment(slackMessageUrlProvider.getUrlOnImport(context))),
      0L,
      setOf(),
      false,
    )
  }
}
