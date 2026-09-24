package io.tolgee.ee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SlackConfigManageService
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SlackConfigServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var slackConfigManageService: SlackConfigManageService

  @Autowired
  lateinit var slackConfigReadService: SlackConfigReadService

  lateinit var testData: SlackTestData

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `deletes configs`() {
    slackConfigManageService.delete(testData.projectBuilder.self.id, testData.slackConfig.channelId, "")
    Assertions.assertThat(slackConfigReadService.findAll()).isEmpty()
  }

  @Test
  fun `creates new config`() {
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

  @Test
  fun `keeps all events on the channel when switching a language subscription to global`() {
    val languageSubscription =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        channelId = "testChannel2",
        userAccount = testData.user,
        languageTag = "fr",
        events = mutableSetOf(SlackEventType.NEW_KEY),
        slackTeamId = "slackTeamId",
      )
    slackConfigManageService.createOrUpdate(languageSubscription)
    slackConfigManageService.createOrUpdate(
      SlackConfigDto(
        project = testData.projectBuilder.self,
        channelId = "testChannel2",
        userAccount = testData.user,
        isGlobal = true,
        slackTeamId = "slackTeamId",
      ),
    )

    executeInNewTransaction {
      val config = slackConfigReadService.find(testData.projectBuilder.self.id, "testChannel2")!!
      config.isGlobalSubscription.assert.isTrue()
      config.events.assert.containsExactly(SlackEventType.ALL)
    }
  }

  @Test
  fun `creates global config with language preference when both are requested`() {
    slackConfigManageService.createOrUpdate(
      SlackConfigDto(
        project = testData.projectBuilder.self,
        channelId = "testChannel2",
        userAccount = testData.user,
        languageTag = "fr",
        events = mutableSetOf(SlackEventType.NEW_KEY),
        isGlobal = true,
        slackTeamId = "slackTeamId",
      ),
    )

    executeInNewTransaction {
      val config = slackConfigReadService.find(testData.projectBuilder.self.id, "testChannel2")!!
      config.isGlobalSubscription.assert.isTrue()
      config.events.assert.containsExactly(SlackEventType.ALL)
      config.preferences.map { it.languageTag to it.events }.assert.containsExactly(
        "fr" to mutableSetOf(SlackEventType.NEW_KEY),
      )
    }
  }
}
