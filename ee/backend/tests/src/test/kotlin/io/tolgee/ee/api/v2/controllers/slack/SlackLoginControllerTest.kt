package io.tolgee.ee.api.v2.controllers.slack

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.slackIntegration.SlackUserLoginUrlProvider
import io.tolgee.ee.service.slackIntegration.SlackUserConnectionService
import io.tolgee.fixtures.andIsOk
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

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeAll
  fun setUp() {
    tolgeeProperties.slack.token = "token"
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SLACK_INTEGRATION)
  }

  @Test
  fun `user logs in`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.dataWithoutSlackUserConnection)

    slackUserLoginUrlProvider.encryptData("ChannelTest", "TEST1", testData.slackWorkspace.id).let {
      performAuthPost("/v2/slack/user-login?data=$it", null).andIsOk
    }

    Assertions.assertThat(slackUserConnectionService.findBySlackId("TEST1")).isNotNull()
  }
}
