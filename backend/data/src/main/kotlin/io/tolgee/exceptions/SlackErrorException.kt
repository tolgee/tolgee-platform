package io.tolgee.api.v2.controllers.slack

import io.tolgee.dtos.response.SlackMessageDto

class SlackErrorException(
  val error: SlackMessageDto,
) : RuntimeException("Validation error")
