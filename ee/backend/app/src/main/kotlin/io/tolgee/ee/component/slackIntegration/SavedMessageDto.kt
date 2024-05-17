package io.tolgee.ee.component.slackIntegration

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock

data class SavedMessageDto(
  var blocks: List<LayoutBlock>,
  val attachments: List<Attachment>,
  val keyId: Long,
  val langTag: Set<String>,
  val createdKeyBlocks: Boolean = false,
  val baseChanged: Boolean = false,
  // map of langTag and authorContext
  val authorContext: Map<String, String> = mapOf(),
)
