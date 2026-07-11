package io.tolgee.ee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SlackConfigManageService
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SlackConfigServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var slackConfigManageService: SlackConfigManageService

  @Autowired
  lateinit var slackConfigReadService: SlackConfigReadService

  @Test
  fun `deletes configs`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    slackConfigManageService.delete(testData.projectBuilder.self.id, testData.slackConfig.channelId, "")
    Assertions.assertThat(slackConfigReadService.findAll()).isEmpty()
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
    slackConfigManageService.createOrUpdate(slackConfigDto)
    Assertions.assertThat(slackConfigReadService.findAll()).hasSize(2)
  }
}
