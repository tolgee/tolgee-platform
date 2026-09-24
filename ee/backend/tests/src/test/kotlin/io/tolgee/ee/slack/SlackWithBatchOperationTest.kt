package io.tolgee.ee.slack

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.block.SectionBlock
import io.tolgee.batch.ApplicationBatchJobRunner
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SlackConfigManageService
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class SlackWithBatchOperationTest : MachineTranslationTest() {
  @Autowired
  @MockitoBean
  lateinit var slackClient: Slack

  @Autowired
  lateinit var applicationBatchJobRunner: ApplicationBatchJobRunner

  @Autowired
  lateinit var slackConfigManageService: SlackConfigManageService

  companion object {
    private const val INITIAL_BUCKET_CREDITS = 150000L
  }

  lateinit var testData: SlackTestData

  @BeforeEach
  fun setup() {
    testData = SlackTestData()
    initMachineTranslationMocks(
      // the delay is here, so we test that it really doesn't send the message on every single change
      // Without the delay it would be so fast that the batch operation would be finished before
      // the first message is sent and so the tests would pass even if the implementation was wrong
      translateDelay = 20,
    )
    initMachineTranslationProperties(INITIAL_BUCKET_CREDITS)
    this.projectSupplier = { testData.projectBuilder.self }
    tolgeeProperties.slack.token = "token"
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends only 1 message per whole operation`() {
    val keys = testData.add10Keys()
    saveTestData()
    val keyIds = keys.map { it.id }
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    performBatchOperation(keyIds)

    waitForNotThrowing(timeout = 60000) {
      val requests = mockedSlackClient.chatPostMessageRequests
      requests.assert.hasSize(1)
      val sectionBlock = requests.single().blocks.first() as SectionBlock
      sectionBlock.text.text.assert
        .contains("has updated 10 translations")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends only 1 message per whole operation (when translations were updated before)`() {
    val keys = testData.add10Keys()
    saveTestData()
    val keyIds = keys.map { it.id }
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    waitFor(pollTime = 5) {
      applicationBatchJobRunner.settled
    }

    mockedSlackClient.clearInvocations()

    keys.take(3).forEach {
      performUpdateTranslation(it.name)
    }

    waitForNotThrowing(timeout = 20_000) {
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(3)
    }

    // Auto-translation batch jobs may still be running and can produce
    // chatUpdate requests (same race as SlackWithAutoTranslationTest).
    // Record the current count so we can assert on the delta after the
    // batch operation, rather than relying on clearInvocations which
    // can miss late-arriving updates.
    val postsBefore = mockedSlackClient.chatPostMessageRequests.size
    val updatesBefore = mockedSlackClient.chatUpdateRequests.size

    performBatchOperation(keyIds)

    waitForNotThrowing(timeout = 10_000) {
      val newPosts = mockedSlackClient.chatPostMessageRequests.size - postsBefore
      val newUpdates = mockedSlackClient.chatUpdateRequests.size - updatesBefore
      newPosts.assert.isEqualTo(1)
      // currently is 6 due to suboptimal implementations
      newUpdates.assert.isEqualTo(3)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not send the big-operation summary to a new_key-only subscription`() {
    testData.slackConfig.isGlobalSubscription = true
    testData.slackConfig.events = mutableSetOf(SlackEventType.NEW_KEY)
    val keys = testData.add10Keys()
    saveTestData()
    val keyIds = keys.map { it.id }
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    waitFor(pollTime = 5) {
      applicationBatchJobRunner.settled
    }
    mockedSlackClient.clearInvocations()

    performBatchOperation(keyIds)
    waitForBatchJobsToSettle()
    performCreateKey("controlKey")

    waitForNotThrowing(timeout = 20_000) {
      val requests = mockedSlackClient.chatPostMessageRequests
      requests.assert.hasSize(1)
      val sectionBlock = requests.single().blocks.first() as SectionBlock
      sectionBlock.text.text.assert
        .doesNotContain("has updated")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends a detailed message instead of the summary when a batch changes one key in many languages`() {
    testData.addMoreLanguages()
    saveTestData()
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    waitForBatchJobsToSettle()
    mockedSlackClient.clearInvocations()

    performBatchOperation(listOf(testData.key.id), nonBaseLanguageIds())

    waitForNotThrowing(timeout = 20_000) {
      val request = mockedSlackClient.chatPostMessageRequests.single()
      (request.blocks.first() as SectionBlock)
        .text.text.assert
        .doesNotContain("has updated")
      request.attachments.assert.hasSize(nonBaseLanguageIds().size + 2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends the summary only to a subscription for a language the batch changed`() {
    val keys = testData.add10Keys()
    saveTestData()
    subscribeToFrenchTranslationChangesOnly()
    val keyIds = keys.map { it.id }
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    waitForBatchJobsToSettle()
    mockedSlackClient.clearInvocations()

    performBatchOperation(keyIds, listOf(languageId("cs")))
    performBatchOperation(keyIds.take(6), listOf(testData.secondLanguage.id))

    waitForNotThrowing(timeout = 20_000) {
      mockedSlackClient.chatPostMessageRequests
        .headerTexts()
        .assert
        .anyMatch { it.contains("has updated 6") }
    }
    waitForBatchJobsToSettle()
    mockedSlackClient.chatPostMessageRequests
      .headerTexts()
      .assert
      .hasSize(1)
  }

  private fun List<ChatPostMessageRequest>.headerTexts(): List<String> =
    map { (it.blocks.first() as SectionBlock).text.text }

  private fun subscribeToFrenchTranslationChangesOnly() {
    slackConfigManageService.delete(testData.projectBuilder.self.id, testData.slackConfig.channelId, "")
    slackConfigManageService.createOrUpdate(
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = testData.slackConfig.channelId,
        userAccount = testData.user,
        languageTag = "fr",
        events = mutableSetOf(SlackEventType.TRANSLATION_CHANGED),
        slackTeamId = "slackTeamId",
      ),
    )
  }

  private fun waitForBatchJobsToSettle() {
    waitFor(pollTime = 5) {
      applicationBatchJobRunner.settled
    }
  }

  private fun nonBaseLanguageIds(): List<Long> =
    testData.projectBuilder.data.languages
      .map { it.self }
      .filter { it.tag != "en" }
      .map { it.id }

  private fun languageId(tag: String): Long =
    testData.projectBuilder.data.languages
      .single { it.self.tag == tag }
      .self.id

  private fun performCreateKey(name: String) {
    performProjectAuthPost(
      "keys/create",
      mapOf("name" to name, "translations" to mapOf("en" to "Hello")),
    ).andIsCreated
  }

  private fun performBatchOperation(
    keyIds: List<Long>,
    targetLanguageIds: List<Long> = listOf(testData.secondLanguage.id),
  ) {
    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf("keyIds" to keyIds, "targetLanguageIds" to targetLanguageIds),
    ).andIsOk
  }

  private fun performUpdateTranslation(key: String) {
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        key = key,
        null,
        mutableMapOf("en" to "Updated"),
      ),
    ).andIsOk
  }
}
