package io.tolgee.ee.component.slackIntegration.notification.messageFactory

import io.tolgee.ee.component.slackIntegration.data.SlackMessageDto
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageContext
import io.tolgee.ee.component.slackIntegration.notification.SlackMessageUrlProvider
import io.tolgee.ee.component.slackIntegration.notification.SlackNotificationBlocksProvider
import org.springframework.stereotype.Component

@Component
class SlackImportMessageFactory(
  private val slackMessageUrlProvider: SlackMessageUrlProvider,
  private val blocksProvider: SlackNotificationBlocksProvider,
) {
  fun createImportMessage(context: SlackMessageContext): SlackMessageDto? {
    val importedCount = context.activityData?.counts?.get("Key") ?: return null

    return SlackMessageDto(
      blocks = blocksProvider.getImportBlocks(context, importedCount),
      attachments = listOf(blocksProvider.getRedirectButtonAttachment(slackMessageUrlProvider.getUrlOnImport(context))),
      0L,
      setOf(),
    )
  }
}
