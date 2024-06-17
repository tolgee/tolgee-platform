package io.tolgee.ee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SlackConfigService
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SlackConfigServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var slackConfigService: SlackConfigService

  @Test
  fun `deletes configs`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    slackConfigService.delete(testData.projectBuilder.self.id, testData.slackConfig.channelId, "")
    Assertions.assertThat(slackConfigService.findAll()).isEmpty()
  }

  @Test
  fun `creates new config`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val slackConfigDto =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        channelId = "testChannel2",
        userAccount = testData.user,
        events = mutableSetOf(SlackEventType.ALL),
        isGlobal = true,
        slackTeamId = "slackTeamId",
      )
    slackConfigService.createOrUpdate(slackConfigDto)
    Assertions.assertThat(slackConfigService.findAll()).hasSize(2)
  }
}
