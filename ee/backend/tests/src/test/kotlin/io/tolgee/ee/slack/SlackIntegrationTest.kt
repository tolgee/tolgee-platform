package io.tolgee.ee.slack

import com.slack.api.Slack
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.ee.service.slackIntegration.SlackConfigManageService
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import io.tolgee.util.Logging
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

  @BeforeAll
  fun setup() {
    tolgeeProperties.internal.fakeMtProviders = false
    tolgeeProperties.machineTranslation.google.apiKey = ""

    tolgeeProperties.slack.token = "token"
  }

  @Test
  fun `sends message to correct channel after translation changed`() {
    val testData = SlackTestData()
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
    val testData = SlackTestData()
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
    val testData = SlackTestData()
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
    val testData = SlackTestData()
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

  private fun modifyTranslationData(
    projectId: Long,
    landTag: String,
    keyName: String,
  ) {
    performAuthPost(
      "/v2/projects/$projectId/translations",
      mapOf(
        "key" to keyName,
        "translations" to mapOf(landTag to UUID.randomUUID().toString()),
      ),
    ).andIsOk
  }

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
