package io.tolgee.api.v2.controllers.slack

import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SlackLoginControllerTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var slackUserConnectionService: SlackUserConnectionService

  @BeforeAll
  fun setUp() {
    tolgeeProperties.slack.token = "token"
  }

  @Test
  fun `user log in`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    performAuthPost(
      "/v2/slack/user-login",
      mapOf(
        "slackId" to testData.slackUserConnection.slackUserId,
        "channelId" to "TEST",
        "workspaceId" to testData.slackWorkspace.id,
      ),
    ).andIsOk
  }

  @Test
  fun `user does not log in and creates new connection`() {
    Assertions.assertThat(slackUserConnectionService.findBySlackId("TEST1")).isNull()
    performAuthPost(
      "/v2/slack/user-login",
      mapOf(
        "slackId" to "TEST1",
        "channelId" to "TEST",
        "workspaceId" to 1,
      ),
    ).andIsOk

    Assertions.assertThat(slackUserConnectionService.findBySlackId("TEST1")).isNotNull()
  }
}
