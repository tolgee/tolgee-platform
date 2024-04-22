package io.tolgee.slack

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

class SlackIntegrationTest : ProjectAuthControllerTest(), Logging {
  @Autowired
  @MockBean
  lateinit var slackClient: Slack

  lateinit var mockedSlackClient: MockedSlackClient

  @Autowired
  lateinit var slackMessageService: SavedSlackMessageService

  @BeforeAll
  fun setUp() {
    // slackProperties.token = "fakeToken"
    // tolgeeProperties.slack.token = "fakeToken"
  }

  @Test
  fun `sends message to correct channel after translation changed`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    mockedSlackClient = mockSlackClient()

    Thread.sleep(1000)
    Mockito.clearInvocations(mockedSlackClient.methodsClientMock)

    val langTag = testData.projectBuilder.self.baseLanguage?.tag ?: ""
    loginAsUser(testData.user.username)
    waitForNotThrowing {
      modifyTranslationData(testData.projectBuilder.self.id, langTag)
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
      val request = mockedSlackClient.chatPostMessageRequests.single()
      request.channel.assert.isEqualTo(testData.slackConfig.channelId)
    }
  }

  @Test
  fun `sends message to correct channel after key added`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    mockedSlackClient = mockSlackClient()

    loginAsUser(testData.user.username)
    waitForNotThrowing {
      addKeyToProject(testData.projectBuilder.self.id)
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
      val request = mockedSlackClient.chatPostMessageRequests.single()
      request.channel.assert.isEqualTo(testData.slackConfig.channelId)
    }
  }

  @Test
  fun `Don't send a message if the subscription isn't global and modified language isn't in the preferred languages`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    mockedSlackClient = mockSlackClient()

    val updatedConfig =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = "testChannel",
        userAccount = testData.user,
        languageTag = "fr",
        onEvent = EventName.ALL,
        slackTeamId = "",
      )
    slackConfigService.delete(testData.slackConfig.project.id, "testChannel")
    val config = slackConfigService.createOrUpdate(updatedConfig)

    loginAsUser(testData.user.username)

    modifyTranslationData(testData.projectBuilder.self.id, "cs")
    mockedSlackClient.chatPostMessageRequests.assert.hasSize(0)
    slackMessageService.findByKey(testData.key.id, config.id).forEach {
      it.langTags.assert.doesNotContain("cs")
    }
  }

  @Test
  fun `Don't send a message if the event isn't in subscribed by user`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    mockedSlackClient = mockSlackClient()

    val updatedConfig =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = "testChannel",
        userAccount = testData.user,
        languageTag = "en",
        onEvent = EventName.TRANSLATION_CHANGED,
        slackTeamId = "",
      )
    slackConfigService.delete(testData.slackConfig.project.id, "testChannel")
    val config = slackConfigService.createOrUpdate(updatedConfig)

    loginAsUser(testData.user.username)

    addKeyToProject(testData.projectBuilder.self.id)
    mockedSlackClient.chatPostMessageRequests.assert.hasSize(0)
    slackMessageService.findByKey(testData.key.id, config.id).forEach {
      it.langTags.assert.doesNotContain("en")
    }
  }

  fun mockSlackClient(): MockedSlackClient {
    val methodsClientMock = mock<MethodsClient>()
    whenever(slackClient.methods(any())).thenReturn(methodsClientMock)
    val mockPostMessageResponse = mock<ChatPostMessageResponse>()
    whenever(mockPostMessageResponse.isOk).thenReturn(true)
    whenever(mockPostMessageResponse.ts).thenReturn("ts")

    whenever(
      methodsClientMock.chatPostMessage(
        any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>(),
      ),
    ).thenReturn(mockPostMessageResponse)

    return MockedSlackClient(methodsClientMock)
  }

  private fun modifyTranslationData(
    projectId: Long,
    landTag: String,
  ) {
    performAuthPost(
      "/v2/projects/$projectId/translations",
      mapOf(
        "key" to "testKey",
        "translations" to mapOf(landTag to UUID.randomUUID().toString()),
      ),
    ).andIsOk
  }

  private fun addKeyToProject(projectId: Long) {
    performAuthPost(
      "/v2/projects/$projectId/translations",
      mapOf(
        "key" to "newKey",
        "translations" to mapOf("en" to "Sample Translation"),
      ),
    ).andIsOk
  }
}
