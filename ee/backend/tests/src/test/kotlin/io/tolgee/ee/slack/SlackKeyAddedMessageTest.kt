package io.tolgee.ee.slack

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.block.SectionBlock
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.ApplicationBatchJobRunner
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

class SlackKeyAddedMessageTest : ProjectAuthControllerTest() {
  @Autowired
  @MockitoBean
  lateinit var slackClient: Slack

  @Autowired
  lateinit var slackMessageService: SavedSlackMessageService

  @Autowired
  lateinit var applicationBatchJobRunner: ApplicationBatchJobRunner

  lateinit var testData: SlackTestData

  lateinit var mockedSlackClient: MockedSlackClient

  @BeforeAll
  fun setupProperties() {
    tolgeeProperties.slack.token = "token"
  }

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    loginAsUser(testData.user.username)
  }

  @AfterEach
  fun cleanup() {
    waitFor(pollTime = 5) { applicationBatchJobRunner.settled }
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `sends each translation once in the new key message`() {
    performAuthPost(
      "/v2/projects/${testData.projectBuilder.self.id}/keys/create",
      mapOf(
        "name" to "multiLanguageKey",
        "translations" to mapOf("en" to "Hello", "fr" to "Bonjour", "cs" to "Ahoj"),
      ),
    ).andIsCreated
    val keyId = keyService.get(testData.projectBuilder.self.id, "multiLanguageKey", null).id

    waitForNotThrowing(timeout = 10_000) {
      mockedSlackClient.chatPostMessageRequests.single().assertListsEachLanguageOnce()
      slackMessageService.find(keyId, testData.slackConfig.id).assert.hasSize(1)
    }
  }

  @Test
  fun `lists each language once when the base translation changes`() {
    performAuthPut(
      "/v2/projects/${testData.projectBuilder.self.id}/translations",
      mapOf("key" to testData.keyWithAllLanguages.name, "translations" to mapOf("en" to "Home")),
    ).andIsOk

    waitForNotThrowing(timeout = 10_000) {
      mockedSlackClient.chatPostMessageRequests.single().assertListsEachLanguageOnce()
      slackMessageService.find(testData.keyWithAllLanguages.id, testData.slackConfig.id).assert.hasSize(1)
    }
  }

  private fun ChatPostMessageRequest.assertListsEachLanguageOnce() {
    val languageLabels = attachments.mapNotNull { (it.blocks.firstOrNull() as? SectionBlock)?.text?.text }
    languageLabels.assert.hasSize(3)
    languageLabels.assert.anyMatch { it.contains("*English*") }
    languageLabels.assert.anyMatch { it.contains("*French*") }
    languageLabels.assert.anyMatch { it.contains("*Czech*") }
  }
}
