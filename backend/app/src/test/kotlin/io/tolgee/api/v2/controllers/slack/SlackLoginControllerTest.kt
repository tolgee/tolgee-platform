package io.tolgee.api.v2.controllers.slack

import io.tolgee.component.automations.processors.slackIntegration.SlackUserLoginUrlProvider
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

  @Autowired
  lateinit var slackUserLoginUrlProvider: SlackUserLoginUrlProvider

  @BeforeAll
  fun setUp() {
    tolgeeProperties.slack.token = "token"
  }

  @Test
  fun `user log in`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)

    slackUserLoginUrlProvider.encryptData("ChannelTest", "TEST1", testData.slackWorkspace.id).let {
      performAuthPost("/v2/slack/user-login?data=$it", mapOf("" to "")).andIsOk
    }

    Assertions.assertThat(slackUserConnectionService.findBySlackId("TEST1")).isNotNull()
  }
}
