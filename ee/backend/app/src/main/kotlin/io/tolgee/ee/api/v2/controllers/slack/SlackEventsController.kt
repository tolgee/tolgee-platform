package io.tolgee.ee.api.v2.controllers.slack

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.slack.SlackEventDto
import io.tolgee.ee.component.slackIntegration.SlackChannelMessagesOperations
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackRequestValidation
import io.tolgee.ee.component.slackIntegration.slashcommand.SlackSlackCommandBlocksProvider
import io.tolgee.ee.service.slackIntegration.OrganizationSlackWorkspaceService
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
  private val slackSlackCommandBlocksProvider: SlackSlackCommandBlocksProvider,
  private val slackChannelMessagesOperations: SlackChannelMessagesOperations,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
) : Logging {
  @Suppress("UastIncorrectHttpHeaderInspection")
  @PostMapping("/on-event")
  @Operation(
    summary = "On interactivity event",
    description =
      "This is triggered when interactivity event is triggered. " +
        "E.g., when user clicks button provided in previous messages.",
  )
  fun onInteractivityEvent(
    @RequestHeader("X-Slack-Signature") slackSignature: String,
    @RequestHeader("X-Slack-Request-Timestamp") timestamp: String,
    @RequestBody payload: String,
  ) {
    val event: SlackEventDto = validateAndParsePayload(payload, slackSignature, timestamp)

    // Since we cannot respond to the event directly, we have to send a message to the channel.
    // Sometimes Tolgee might be unable to do it, because the token might be missing in Tolgee DB.
    // In that case we cannot do anything and the event triggering button in Slack just won't work
    try {
      event.actions.forEach {
        when (it.value) {
          "help_btn" ->
            slackChannelMessagesOperations.sendMessage(
              SlackChannelMessagesOperations.SlackTeamId(event.team.id),
              event.channel.id,
              slackSlackCommandBlocksProvider.getHelpBlocks(),
            )

          "help_advanced_subscribe_btn" ->
            slackChannelMessagesOperations.sendMessage(
              SlackChannelMessagesOperations.SlackTeamId(event.team.id),
              event.channel.id,
              slackSlackCommandBlocksProvider.getAdvancedSubscriptionHelpBlocks(),
            )
        }
      }
    } catch (e: SlackErrorException) {
      slackChannelMessagesOperations.sendMessage(
        SlackChannelMessagesOperations.SlackTeamId(event.team.id),
        event.channel.id,
        e.blocks,
      )
    }
  }

  @Suppress("UastIncorrectHttpHeaderInspection")
  @PostMapping("/on-bot-event")
  @Operation(
    summary = "On bot event",
    description =
      "This is triggered when bot event is triggered. " +
        "E.g., when app is uninstalled from workspace. \n\n" +
        "Heads up! The events have to be configured via Slack App configuration in " +
        "Event Subscription section.",
  )
  fun fetchBotEvent(
    @RequestHeader("X-Slack-Signature") slackSignature: String,
    @RequestHeader("X-Slack-Request-Timestamp") timestamp: String,
    @RequestBody payload: String,
  ): Any? {
    val data: Map<String, Any?> = validateAndParsePayload(payload, slackSignature, timestamp)

    if (data["challenge"] != null) {
      return data["challenge"]
    }

    val event = data["event"] as? Map<*, *> ?: return null
    val eventType = event["type"] ?: return null

    when (eventType) {
      "app_uninstalled" -> {
        val teamId = data["team_id"] as? String ?: return null
        organizationSlackWorkspaceService.deleteWorkspaceWhenUninstalled(teamId)
      }
    }

    return null
  }

  private inline fun <reified T> validateAndParsePayload(
    payload: String,
    slackSignature: String,
    timestamp: String,
  ): T {
    val decodedPayload = URLDecoder.decode(payload.substringAfter("="), "UTF-8")
    if (decodedPayload.contains("\"value\":\"redirect\"")) {
      return objectMapper.readValue(decodedPayload)
    }

    slackRequestValidation.validate(slackSignature, timestamp, payload)
    return objectMapper.readValue(decodedPayload)
  }
}
