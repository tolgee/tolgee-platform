package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.Attachment
import com.slack.api.model.block.LayoutBlock

data class SavedMessageDto(
  val blocks: List<LayoutBlock>,
  val attachments: List<Attachment>,
  val keyId: Long,
  val langTag: Set<String>
)
