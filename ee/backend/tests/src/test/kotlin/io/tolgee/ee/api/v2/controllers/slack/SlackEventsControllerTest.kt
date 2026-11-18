package io.tolgee.ee.api.v2.controllers.slack

import com.slack.api.Slack
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.component.slackIntegration.SlackChannelMessagesOperations
import io.tolgee.ee.repository.slackIntegration.OrganizationSlackWorkspaceRepository
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SlackEventsControllerTest : AuthorizedControllerTest() {
  @Autowired
  @MockitoBean
  lateinit var slackClient: Slack

  @Autowired
  lateinit var organizationSlackWorkspaceRepository: OrganizationSlackWorkspaceRepository

  @Autowired
  @MockitoBean
  lateinit var slackChannelMessagesOperations: SlackChannelMessagesOperations

  @BeforeAll
  fun setUp() {
    tolgeeProperties.slack.token = "token"
    tolgeeProperties.slack.signingSecret = "fakeSecret"
  }

  @Test
  fun `forged bot event with redirect marker does not bypass signature validation`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    // A forged, unsigned payload that smuggles the "redirect" marker together with an
    // app_uninstalled event for a known team id. It must not delete the workspace.
    val payload =
      """{"value":"redirect","team_id":"slackTeamId","event":{"type":"app_uninstalled"}}"""

    performSlackPost("/on-bot-event", payload, signature = "v0=bogus", timestamp = "0").andIsBadRequest

    organizationSlackWorkspaceRepository
      .findBySlackTeamId("slackTeamId")
      .assert
      .describedAs("workspace must not be deleted by an unverified request")
      .isNotNull
  }

  @Test
  fun `properly signed app_uninstalled bot event deletes the workspace`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    val payload = """{"team_id":"slackTeamId","event":{"type":"app_uninstalled"}}"""
    val timestamp = Instant.now().epochSecond.toString()

    performSlackPost("/on-bot-event", payload, signature = sign(payload, timestamp), timestamp = timestamp).andIsOk

    organizationSlackWorkspaceRepository
      .findBySlackTeamId("slackTeamId")
      .assert
      .describedAs("a valid signed uninstall event should still remove the workspace")
      .isNull()
  }

  @Test
  fun `unsigned interactivity payload with redirect marker is rejected and runs no action`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    // A forged, unsigned payload smuggling the "redirect" marker next to a help button for a known team.
    val body = interactivityBody("redirect", "help_btn")

    performSlackPost("/on-event", body, signature = "v0=bogus", timestamp = "0").andIsBadRequest

    verify(slackChannelMessagesOperations, never()).sendMessage(any(), any(), any(), anyOrNull())
  }

  @Test
  fun `signed redirect button payload is accepted and sends nothing`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    val body = interactivityBody("redirect")
    val timestamp = Instant.now().epochSecond.toString()

    performSlackPost("/on-event", body, signature = sign(body, timestamp), timestamp = timestamp).andIsOk

    verify(slackChannelMessagesOperations, never()).sendMessage(any(), any(), any(), anyOrNull())
  }

  @Test
  fun `signed help button payload sends the help message`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    val body = interactivityBody("help_btn")
    val timestamp = Instant.now().epochSecond.toString()

    performSlackPost("/on-event", body, signature = sign(body, timestamp), timestamp = timestamp).andIsOk

    verify(slackChannelMessagesOperations).sendMessage(
      eq(SlackChannelMessagesOperations.SlackTeamId("slackTeamId")),
      eq("testChannel"),
      any(),
      anyOrNull(),
    )
  }

  private fun interactivityBody(vararg actionValues: String): String {
    val json =
      objectMapper.writeValueAsString(
        mapOf(
          "token" to "t",
          "user" to mapOf("id" to "U1"),
          "type" to "block_actions",
          "channel" to mapOf("id" to "testChannel"),
          "actions" to
            actionValues.map {
              mapOf("action_id" to "a", "block_id" to "b", "value" to it, "type" to "button")
            },
          "trigger_id" to "tr",
          "team" to mapOf("id" to "slackTeamId", "domain" to "d"),
        ),
      )
    return "payload=" + URLEncoder.encode(json, StandardCharsets.UTF_8)
  }

  private fun performSlackPost(
    path: String,
    body: String,
    signature: String,
    timestamp: String,
  ) = perform(
    MockMvcRequestBuilders
      .post("/v2/public/slack$path")
      .header("X-Slack-Signature", signature)
      .header("X-Slack-Request-Timestamp", timestamp)
      .contentType(MediaType.TEXT_PLAIN)
      .content(body)
      .headers(HttpHeaders.EMPTY),
  )

  private fun sign(
    body: String,
    timestamp: String,
  ): String {
    val secret = tolgeeProperties.slack.signingSecret!!
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
    val hash = mac.doFinal("v0:$timestamp:$body".toByteArray(StandardCharsets.UTF_8))
    return "v0=" + hash.joinToString("") { "%02x".format(it.toInt() and 0xff) }
  }
}
