package io.tolgee.ee.api.v2.controllers.slack

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.request.conversations.ConversationsInfoRequest
import com.slack.api.methods.response.conversations.ConversationsInfoResponse
import com.slack.api.model.Conversation
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.ee.slack.MockedSlackClient
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SlackSlashCommandControllerTest : AuthorizedControllerTest() {
  @Autowired
  @MockitoBean
  lateinit var slackClient: Slack

  @Autowired
  lateinit var slackConfigReadService: SlackConfigReadService

  lateinit var testData: SlackTestData

  @BeforeAll
  fun setupProperties() {
    tolgeeProperties.slack.token = "token"
    tolgeeProperties.slack.signingSecret = "fakeSecret"
  }

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    whenever(
      mockedSlackClient.methodsClientMock.conversationsInfo(
        any<RequestConfigurator<ConversationsInfoRequest.ConversationsInfoRequestBuilder>>(),
      ),
    ).thenReturn(
      ConversationsInfoResponse().also {
        it.isOk = true
        it.channel = Conversation()
      },
    )
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `keeps the subscription when unsubscribe has a language tag glued to the project id`() {
    performSlashCommand("unsubscribe ${testData.projectBuilder.self.id}fr").andIsOk

    slackConfigReadService
      .find(testData.projectBuilder.self.id, testData.slackConfig.channelId)
      .assert
      .isNotNull()
  }

  private fun performSlashCommand(text: String): ResultActions {
    val fields =
      mapOf(
        "team_id" to "slackTeamId",
        "channel_id" to testData.slackConfig.channelId,
        "command" to "/tolgee",
        "channel_name" to "test",
        "user_id" to "slackUserId",
        "user_name" to "user",
        "text" to text,
        "team_domain" to "team",
      )
    val body =
      fields.entries.joinToString("&") { (name, value) ->
        "$name=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
      }
    val timestamp = Instant.now().epochSecond.toString()
    return perform(
      MockMvcRequestBuilders
        .post("/v2/public/slack")
        .header("X-Slack-Signature", sign(body, timestamp))
        .header("X-Slack-Request-Timestamp", timestamp)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .content(body),
    )
  }

  private fun sign(
    body: String,
    timestamp: String,
  ): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(tolgeeProperties.slack.signingSecret!!.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
    val hash = mac.doFinal("v0:$timestamp:$body".toByteArray(StandardCharsets.UTF_8))
    return "v0=" + hash.joinToString("") { "%02x".format(it.toInt() and 0xff) }
  }
}
