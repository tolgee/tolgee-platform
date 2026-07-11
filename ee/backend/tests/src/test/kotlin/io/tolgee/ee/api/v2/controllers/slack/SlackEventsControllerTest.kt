package io.tolgee.ee.api.v2.controllers.slack

import com.slack.api.Slack
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.repository.slackIntegration.OrganizationSlackWorkspaceRepository
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
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

    performBotEvent(payload, signature = "v0=bogus", timestamp = "0").andIsBadRequest

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

    performBotEvent(payload, signature = sign(payload, timestamp), timestamp = timestamp).andIsOk

    organizationSlackWorkspaceRepository
      .findBySlackTeamId("slackTeamId")
      .assert
      .describedAs("a valid signed uninstall event should still remove the workspace")
      .isNull()
  }

  private fun performBotEvent(
    payload: String,
    signature: String,
    timestamp: String,
  ) = perform(
    MockMvcRequestBuilders
      .post("/v2/public/slack/on-bot-event")
      .header("X-Slack-Signature", signature)
      .header("X-Slack-Request-Timestamp", timestamp)
      .contentType(MediaType.TEXT_PLAIN)
      .content(payload)
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
