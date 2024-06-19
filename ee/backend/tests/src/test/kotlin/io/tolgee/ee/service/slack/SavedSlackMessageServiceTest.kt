package io.tolgee.ee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.testing.assertions.Assertions
import io.tolgee.util.addMinutes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SavedSlackMessageServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var savedSlackMessageService: SavedSlackMessageService

  @AfterEach
  fun after() {
    currentDateProvider.forcedDate = null
  }

  @Test
  fun `deletes old messages`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(125)

    savedSlackMessageService.deleteOldMessage()
    Assertions.assertThat(savedSlackMessageService.findAll()).isEmpty()
  }

  @Test
  fun `finds messages`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val result = savedSlackMessageService.find(0L, testData.slackConfig.id)

    Assertions.assertThat(result).hasSize(2)
  }
}
