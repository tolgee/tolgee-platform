package io.tolgee.ee.slack

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.block.ContextBlock
import com.slack.api.model.block.SectionBlock
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.ee.service.slackIntegration.SlackConfigManageService
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import io.tolgee.util.Logging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

class SlackIntegrationTest :
  ProjectAuthControllerTest(),
  Logging {
  @Autowired
  @MockitoBean
  lateinit var slackClient: Slack

  @Autowired
  lateinit var slackMessageService: SavedSlackMessageService

  @Autowired
  lateinit var slackConfigManageService: SlackConfigManageService

  lateinit var testData: SlackTestData

  @BeforeAll
  fun setup() {
    tolgeeProperties.internal.fakeMtProviders = false
    tolgeeProperties.machineTranslation.google.apiKey = ""

    tolgeeProperties.slack.token = "token"
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `sends message to correct channel after translation changed`() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    val langTag =
      testData.projectBuilder.self.baseLanguage
        ?.tag ?: ""
    loginAsUser(testData.user.username)
    Mockito.clearInvocations(mockedSlackClient.methodsClientMock)
    modifyTranslationData(testData.projectBuilder.self.id, langTag, testData.key.name)
    waitForNotThrowing(timeout = 3000) {
      val request = mockedSlackClient.chatPostMessageRequests.first()
      request.channel.assert.isEqualTo(testData.slackConfig.channelId)
      Assertions.assertThat(slackMessageService.find(testData.key.id, testData.slackConfig.id)).hasSize(1)
    }
  }

  @Test
  fun `sends message to correct channel after key added`() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    loginAsUser(testData.user.username)
    addKeyToProject(testData.projectBuilder.self.id)
    waitForNotThrowing(timeout = 3000) {
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
      val request = mockedSlackClient.chatPostMessageRequests.first()
      request.channel.assert.isEqualTo(testData.slackConfig.channelId)
    }
  }

  @Test
  fun `doesn't send a message if the subscription isn't global and modified language isn't in preferred languages`() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    val updatedConfig =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = "testChannel",
        userAccount = testData.user,
        languageTag = "fr",
        events = mutableSetOf(SlackEventType.ALL),
        slackTeamId = "slackTeamId",
      )
    slackConfigManageService.delete(testData.slackConfig.project.id, "testChannel", "")
    val config = slackConfigManageService.createOrUpdate(updatedConfig)

    loginAsUser(testData.user.username)

    modifyTranslationData(testData.projectBuilder.self.id, "cs", testData.key2.name)
    mockedSlackClient.chatPostMessageRequests.assert.hasSize(0)
    slackMessageService.find(testData.key2.id, config.id).forEach {
      it.languageTags.assert.doesNotContain("cs")
    }
  }

  @Test
  fun `doesn't send a message if the event isn't in subscribed by user`() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)

    val updatedConfig =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = "testChannel",
        userAccount = testData.user,
        languageTag = "en",
        events = mutableSetOf(SlackEventType.TRANSLATION_CHANGED),
        slackTeamId = "slackTeamId",
      )
    slackConfigManageService.delete(testData.slackConfig.project.id, "testChannel", "")
    val config = slackConfigManageService.createOrUpdate(updatedConfig)

    loginAsUser(testData.user.username)

    addKeyToProject(testData.projectBuilder.self.id)
    mockedSlackClient.chatPostMessageRequests.assert.hasSize(0)
    slackMessageService.find(testData.key2.id, config.id).forEach {
      it.languageTags.assert.doesNotContain("en")
    }
  }

  @Test
  fun `sends one message with all changed languages when one key changes in many languages`() {
    testData = SlackTestData()
    testData.addMoreLanguages()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    loginAsUser(testData.user.username)

    setTranslations(
      testData.projectBuilder.self.id,
      testData.key.name,
      mapOf("fr" to "a", "cs" to "b", "de" to "c", "es" to "d", "it" to "e", "pl" to "f"),
    )

    waitForNotThrowing(timeout = 5000) {
      val request = mockedSlackClient.chatPostMessageRequests.single()
      request.headerText().assert.contains("has changed translations in")
      request.languageLabels().assert.contains(
        "*French*",
        "*Czech*",
        "*German*",
        "*Spanish*",
        "*Italian*",
        "*Polish*",
      )
    }
  }

  @Test
  fun `sends message for subscribed language saved together with unsubscribed language of lower id`() {
    testData = SlackTestData()
    lateinit var czechTranslation: Translation
    lateinit var frenchTranslation: Translation
    val orderKey =
      testData.projectBuilder
        .addKey("orderKey")
        .build {
          czechTranslation = addTranslation("cs", "Ahoj").self
          frenchTranslation = addTranslation("fr", "Salut").self
        }.self
    testDataService.saveTestData(testData.root)
    czechTranslation.id.assert.isLessThan(frenchTranslation.id)
    slackConfigManageService.delete(testData.slackConfig.project.id, "testChannel", "")
    slackConfigManageService.createOrUpdate(
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = "testChannel",
        userAccount = testData.user,
        languageTag = "fr",
        events = mutableSetOf(SlackEventType.TRANSLATION_CHANGED),
        slackTeamId = "slackTeamId",
      ),
    )
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    loginAsUser(testData.user.username)

    setTranslations(testData.projectBuilder.self.id, orderKey.name, mapOf("cs" to "Nazdar", "fr" to "Bonjour"))

    waitForNotThrowing(timeout = 5000) {
      val request = mockedSlackClient.chatPostMessageRequests.single()
      request.languageLabels().assert.contains("*French*")
      request.languageLabels().assert.doesNotContain("*Czech*")
    }
  }

  @Test
  fun `describes base text change as a translation change when a sibling translation has lower id`() {
    testData = SlackTestData()
    lateinit var frenchTranslation: Translation
    lateinit var englishTranslation: Translation
    val siblingKey =
      testData.projectBuilder
        .addKey("siblingKey")
        .build {
          frenchTranslation = addTranslation("fr", "Salut").self
          englishTranslation = addTranslation("en", "Hello").self
        }.self
    testDataService.saveTestData(testData.root)
    frenchTranslation.id.assert.isLessThan(englishTranslation.id)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    loginAsUser(testData.user.username)

    setTranslations(testData.projectBuilder.self.id, siblingKey.name, mapOf("en" to "Hi"))

    waitForNotThrowing(timeout = 5000) {
      mockedSlackClient.chatPostMessageRequests
        .single()
        .headerText()
        .assert
        .contains("has changed a base language translation")
    }
  }

  @Test
  fun `shows author on non-base language saved together with base language`() {
    testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = MockedSlackClient.mockSlackClient(slackClient)
    loginAsUser(testData.user.username)

    setTranslations(testData.projectBuilder.self.id, testData.key2.name, mapOf("en" to "Hi", "fr" to "Salut"))

    waitForNotThrowing(timeout = 5000) {
      val frenchAttachment =
        mockedSlackClient.chatPostMessageRequests.single().attachments.single {
          (it.blocks.firstOrNull() as? SectionBlock)?.text?.text?.contains("*French*") == true
        }
      frenchAttachment.blocks.assert.anyMatch { it is ContextBlock }
    }
  }

  private fun ChatPostMessageRequest.headerText(): String = (blocks.first() as SectionBlock).text.text

  private fun ChatPostMessageRequest.languageLabels(): List<String> =
    attachments.mapNotNull {
      (it.blocks.firstOrNull() as? SectionBlock)
        ?.text
        ?.text
        ?.removePrefix("null ")
        ?.trim()
    }

  private fun setTranslations(
    projectId: Long,
    keyName: String,
    translations: Map<String, String>,
  ) {
    performAuthPost(
      "/v2/projects/$projectId/translations",
      mapOf("key" to keyName, "translations" to translations),
    ).andIsOk
  }

  private fun modifyTranslationData(
    projectId: Long,
    landTag: String,
    keyName: String,
  ) = setTranslations(projectId, keyName, mapOf(landTag to UUID.randomUUID().toString()))

  private fun addKeyToProject(projectId: Long) {
    performAuthPost(
      "/v2/projects/$projectId/keys/create",
      mapOf(
        "name" to "newKey",
        "translations" to mapOf("en" to "Sample Translation"),
      ),
    ).andIsCreated
  }
}
