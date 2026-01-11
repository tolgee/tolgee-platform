package io.tolgee.dtos.response

import com.slack.api.model.block.LayoutBlock

data class SlackMessageDto(
  val text: String? = null,
  val blocks: List<LayoutBlock>? = null,
)
