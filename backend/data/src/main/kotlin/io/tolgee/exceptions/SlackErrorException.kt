package io.tolgee.exceptions

import com.slack.api.model.block.LayoutBlock

class SlackErrorException(
  val blocks: List<LayoutBlock>,
  message: String = "Error while processing slack command",
) : BadRequestException(message)
