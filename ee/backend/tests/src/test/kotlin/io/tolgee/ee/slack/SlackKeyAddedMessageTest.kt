package io.tolgee.ee.slack

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.Attachment
import com.slack.api.model.block.ActionsBlock
import com.slack.api.model.block.SectionBlock
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.ApplicationBatchJobRunner
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.ee.component.slackIntegration.notification.MAX_SLACK_MESSAGE_ATTACHMENTS
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

  @Test
  fun `keeps the new key message within the Slack attachment limit`() {
    val languageTags = createLanguages()

    val keyId = createKeyInAllLanguages(languageTags)

    waitForNotThrowing(timeout = 10_000) {
      val attachments = mockedSlackClient.chatPostMessageRequests.single().attachments
      attachments.assertFitsAttachmentLimit(hiddenCount = 5)
      val savedMessage = slackMessageService.find(keyId, testData.slackConfig.id).single()
      savedMessage.languageTags.assert.containsAll(languageTags)
    }
  }

  @Test
  fun `keeps the changed language visible when updating a message over the Slack attachment limit`() {
    val languageTags = createLanguages()
    val keyId = createKeyInAllLanguages(languageTags)
    waitForNotThrowing(timeout = 10_000) {
      slackMessageService.find(keyId, testData.slackConfig.id).assert.hasSize(1)
    }

    // sorted by name, this language would be the first one to not fit into the message
    val lastLanguageTag = languageTags.last()
    performAuthPut(
      "/v2/projects/${testData.projectBuilder.self.id}/translations",
      mapOf("key" to KEY_IN_ALL_LANGUAGES, "translations" to mapOf(lastLanguageTag to "Changed text")),
    ).andIsOk

    waitForNotThrowing(timeout = 10_000) {
      val attachments = mockedSlackClient.chatUpdateRequests.single().attachments
      attachments.assertFitsAttachmentLimit(hiddenCount = 5)
      attachments.map { it.blocks.toString() }.assert.anyMatch { it.contains("Changed text") }
    }
  }

  /** Together with the 3 languages from the test data, that's 5 languages more than fit into a message */
  private fun createLanguages(): List<String> {
    val project = testData.projectBuilder.self
    val tags =
      (1..100).map { number ->
        val tag = "x-%03d".format(number)
        executeInNewTransaction {
          languageService.createLanguage(
            LanguageRequest(name = "Language $tag", originalName = "Language $tag", tag = tag),
            project,
          )
        }
        tag
      }
    return listOf("en", "fr", "cs") + tags
  }

  private fun createKeyInAllLanguages(languageTags: List<String>): Long {
    performAuthPost(
      "/v2/projects/${testData.projectBuilder.self.id}/keys/create",
      mapOf(
        "name" to KEY_IN_ALL_LANGUAGES,
        "translations" to languageTags.associateWith { "Text in $it" },
      ),
    ).andIsCreated
    return keyService.get(testData.projectBuilder.self.id, KEY_IN_ALL_LANGUAGES, null).id
  }

  private fun List<Attachment>.assertFitsAttachmentLimit(hiddenCount: Int) {
    assert.hasSize(MAX_SLACK_MESSAGE_ATTACHMENTS)
    val blocks = map { it.blocks.toString() }
    blocks.first().assert.contains("(base)")
    blocks[blocks.lastIndex - 1].assert.contains("$hiddenCount more")
    last().blocks.assert.hasOnlyElementsOfType(ActionsBlock::class.java)
  }

  private fun ChatPostMessageRequest.assertListsEachLanguageOnce() {
    val languageLabels = attachments.mapNotNull { (it.blocks.firstOrNull() as? SectionBlock)?.text?.text }
    languageLabels.assert.hasSize(3)
    languageLabels.assert.anyMatch { it.contains("*English*") }
    languageLabels.assert.anyMatch { it.contains("*French*") }
    languageLabels.assert.anyMatch { it.contains("*Czech*") }
  }

  companion object {
    private const val KEY_IN_ALL_LANGUAGES = "keyInAllLanguages"
  }
}
