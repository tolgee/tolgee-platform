package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.dtos.request.slack.SlackEventDto
import io.tolgee.dtos.response.SlackMessageDto
import org.springframework.web.bind.annotation.*
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
  private val slackExecutor: SlackExecutor,
) {
  @PostMapping("/on-event")
  fun fetchEvent(
    @RequestBody payload: String,
  ): SlackMessageDto? {
    val decodedPayload = URLDecoder.decode(payload.substringAfter("="), "UTF-8")
    val event: SlackEventDto = objectMapper.readValue(decodedPayload)

    event.actions.forEach { action ->
      if (action.value != "help_btn") {
        return@forEach
      } else {
        slackExecutor.sendHelpMessage(event.channel.id)
      }
    }

    return null
  }
}
