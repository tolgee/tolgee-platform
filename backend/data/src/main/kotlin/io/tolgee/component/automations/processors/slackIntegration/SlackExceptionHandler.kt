package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.dtos.response.SlackMessageDto
import io.tolgee.exceptions.SlackErrorException
import org.springframework.stereotype.Component

@Component
class SlackExceptionHandler() {
  fun handle(fn: () -> SlackMessageDto?): SlackMessageDto? {
    return try {
      fn()
    } catch (e: SlackErrorException) {
      e.blocks.asSlackMessageDto
    }
  }
}
