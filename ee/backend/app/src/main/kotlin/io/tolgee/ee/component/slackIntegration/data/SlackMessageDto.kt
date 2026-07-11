package io.tolgee.ee.component.slackIntegration.data

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock

data class SlackMessageDto(
  var blocks: List<LayoutBlock>,
  val attachments: List<Attachment>,
  val keyId: Long,
  val languageTags: Set<String>,
  val createdKeyBlocks: Boolean = false,
  val baseChanged: Boolean = false,
  // map of langTag and authorContext
  val authorContext: Map<String, String> = mapOf(),
)
