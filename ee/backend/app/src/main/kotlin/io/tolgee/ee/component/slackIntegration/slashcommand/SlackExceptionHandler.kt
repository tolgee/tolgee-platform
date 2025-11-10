package io.tolgee.ee.component.slackIntegration.slashcommand

import io.tolgee.exceptions.SlackErrorException
import org.springframework.stereotype.Component

@Component
class SlackExceptionHandler {
  fun handle(fn: () -> String?): String? {
    return try {
      fn()
    } catch (e: SlackErrorException) {
      e.blocks.asSlackResponseString
    }
  }
}
