package io.tolgee.api.v2.controllers.slack

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.SlackRequestValidation
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.component.automations.processors.slackIntegration.SlackHelpBlocksProvider
import io.tolgee.dtos.request.slack.SlackEventDto
import io.tolgee.exceptions.SlackErrorException
import io.tolgee.util.Logging
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/public/slack"])
@Tag(
  name = "Slack events",
  description = "Listens for Slack events, such as button clicks, and processes them",
)
class SlackEventsController(
  private val objectMapper: ObjectMapper,
  private val slackRequestValidation: SlackRequestValidation,
  private val slackHelpBlocksProvider: SlackHelpBlocksProvider,
  private val slackExecutor: SlackExecutor,
) : Logging {
  @Suppress("UastIncorrectHttpHeaderInspection")
  @PostMapping("/on-event")
  fun fetchEvent(
    @RequestHeader("X-Slack-Signature") slackSignature: String,
    @RequestHeader("X-Slack-Request-Timestamp") timestamp: String,
    @RequestBody payload: String,
  ) {
    val decodedPayload = URLDecoder.decode(payload.substringAfter("="), "UTF-8")

    slackRequestValidation.validate(slackSignature, timestamp, payload)

    val event: SlackEventDto = objectMapper.readValue(decodedPayload)

    // Since we cannot respond to the event directly, we have to send a message to the channel.
    // Sometimes Tolgee might be unable to do it, because the token might be missing in Tolgee DB.
    // In that case we cannot do anything and the event triggering button in Slack just won't work
    try {
      val hasHelpBtn =
        event.actions.any {
          it.value == "help_btn"
        }
      if (hasHelpBtn) {
        slackExecutor.sendBlocksMessage(event.team.id, event.channel.id, slackHelpBlocksProvider.getHelpBlocks())
      }
    } catch (e: SlackErrorException) {
      slackExecutor.sendBlocksMessage(event.team.id, event.channel.id, e.blocks)
    }
  }
}
