package io.tolgee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test

class SlackConfigServiceTest : AbstractSpringTest() {
  @Test
  fun `deletes configs`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    slackConfigService.delete(testData.projectBuilder.self.id, testData.slackConfig.channelId)
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
        onEvent = EventName.ALL,
      )
    slackConfigService.createOrUpdate(slackConfigDto)
    Assertions.assertThat(slackConfigService.findAll()).hasSize(2)
  }
}
