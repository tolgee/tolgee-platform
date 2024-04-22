package io.tolgee.service.slack

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.testing.assertions.Assertions
import io.tolgee.util.addMinutes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SavedSlackMessageServiceTest : AbstractSpringTest() {
  @BeforeAll
  fun setUp() {
    // createSlackMessage(testData)
  }

  @AfterEach
  fun after() {
    currentDateProvider.forcedDate = null
  }

  @Test
  fun `correct delete old messages`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    currentDateProvider.forcedDate = currentDateProvider.date.addMinutes(125)

    savedSlackMessageService.deleteOldMessage()
    Assertions.assertThat(savedSlackMessageService.findAll()).isEmpty()
  }

  @Test
  fun `correct find messages`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    var result = savedSlackMessageService.find(0L, setOf("fr"), testData.slackConfig.id)

    Assertions.assertThat(result).hasSize(2)

    result = savedSlackMessageService.find(1L, setOf("cz"), testData.slackConfig.id)

    Assertions.assertThat(result).hasSize(1)

    result = savedSlackMessageService.find(52L, setOf("fr", "cz"), testData.slackConfig.id)

    Assertions.assertThat(result).hasSize(1)

    result = savedSlackMessageService.find(1415L, setOf("fr", "cz"), testData.slackConfig.id)

    Assertions.assertThat(result).hasSize(0)
  }
}
