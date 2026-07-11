package io.tolgee.ee.api.v2.controllers.slack

import com.slack.api.Slack
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.SlackNoUserConnectionTestData
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.slackIntegration.SlackUserLoginUrlProvider
import io.tolgee.ee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.ee.slack.MockedSlackClient
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.ContextRecreatingTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

@ContextRecreatingTest
class SlackLoginControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var slackUserConnectionService: SlackUserConnectionService

  @Autowired
  lateinit var slackUserLoginUrlProvider: SlackUserLoginUrlProvider

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  @MockitoBean
  lateinit var slackClient: Slack

  @BeforeAll
  fun setUp() {
    tolgeeProperties.slack.token = "token"
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SLACK_INTEGRATION)
  }

  @Test
  fun `user logs in`() {
    val testData = SlackNoUserConnectionTestData()
    testDataService.saveTestData(testData.root)

    MockedSlackClient.mockSlackClient(slackClient)

    slackUserLoginUrlProvider
      .encryptData(
        "ChannelTest",
        "TEST1",
        testData.slackWorkspace.id,
        testData.slackWorkspace.slackTeamId,
      ).let {
        performAuthPost("/v2/slack/user-login?data=$it", null).andIsOk
      }

    Assertions
      .assertThat(
        slackUserConnectionService.findBySlackId("TEST1", testData.slackWorkspace.slackTeamId),
      ).isNotNull()
  }

  @Test
  fun `should not allow duplicate SlackUserConnection for same Tolgee account and workspace`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    MockedSlackClient.mockSlackClient(slackClient)

    slackUserLoginUrlProvider
      .encryptData(
        "ChannelTest",
        "TEST1",
        testData.slackWorkspace.id,
        testData.slackWorkspace.slackTeamId,
      ).let {
        performAuthPost("/v2/slack/user-login?data=$it", null).andIsBadRequest
      }

    assertThatCode {
      slackUserConnectionService.findBySlackId("TEST1", testData.slackWorkspace.slackTeamId)
    }.doesNotThrowAnyException()
  }

  @Test
  fun `logs in for same Tolgee account and different workspace`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    MockedSlackClient.mockSlackClient(slackClient)

    slackUserLoginUrlProvider
      .encryptData(
        "ChannelTest",
        "slackUserId",
        testData.slackWorkspace2.id,
        testData.slackWorkspace2.slackTeamId,
      ).let {
        performAuthPost("/v2/slack/user-login?data=$it", null).andIsOk
      }

    Assertions
      .assertThat(
        slackUserConnectionService.findBySlackId("slackUserId", testData.slackWorkspace2.slackTeamId),
      ).isNotNull()
  }

  @Test
  fun `should not allow duplicate SlackUserConnection for same Tolgee account and workspace and different Slack acc`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    slackUserLoginUrlProvider
      .encryptData(
        "ChannelTest",
        "slackUserId322",
        testData.slackWorkspace.id,
        testData.slackWorkspace.slackTeamId,
      ).let {
        performAuthPost("/v2/slack/user-login?data=$it", null).andIsBadRequest
      }

    assertThatCode {
      slackUserConnectionService.findBySlackId(
        testData.slackUserConnection.slackUserId,
        testData.slackWorkspace.slackTeamId,
      )
    }.doesNotThrowAnyException()
  }
}
