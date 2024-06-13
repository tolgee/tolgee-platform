package io.tolgee.ee.slack

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.users.UsersInfoResponse
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.dtos.slackintegration.SlackConfigDto
import io.tolgee.ee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.ee.service.slackIntegration.SlackConfigService
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions
import io.tolgee.util.Logging
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

  @Autowired
  lateinit var slackMessageService: SavedSlackMessageService

  @Autowired
  lateinit var slackConfigService: SlackConfigService

  @Test
  fun `sends message to correct channel after translation changed`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = mockSlackClient()

    val langTag = testData.projectBuilder.self.baseLanguage?.tag ?: ""
    loginAsUser(testData.user.username)
    Mockito.clearInvocations(mockedSlackClient.methodsClientMock)
    waitForNotThrowing(timeout = 3000) {
      modifyTranslationData(testData.projectBuilder.self.id, langTag, testData.key.name)
      val request = mockedSlackClient.chatPostMessageRequests.first()
      request.channel.assert.isEqualTo(testData.slackConfig.channelId)
    }
    Assertions.assertThat(slackMessageService.find(testData.key.id, testData.slackConfig.id)).hasSize(1)
  }

  @Test
  fun `sends message to correct channel after key added`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = mockSlackClient()

    loginAsUser(testData.user.username)
    waitForNotThrowing(timeout = 3000) {
      addKeyToProject(testData.projectBuilder.self.id)
      mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
      val request = mockedSlackClient.chatPostMessageRequests.first()
      request.channel.assert.isEqualTo(testData.slackConfig.channelId)
    }
  }

  @Test
  fun `Doesn't send a message if the subscription isn't global and modified language isn't in preferred languages`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = mockSlackClient()

    val updatedConfig =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = "testChannel",
        userAccount = testData.user,
        languageTag = "fr",
        events = mutableSetOf(EventName.ALL),
        slackTeamId = "",
      )
    slackConfigService.delete(testData.slackConfig.project.id, "testChannel", "")
    val config = slackConfigService.createOrUpdate(updatedConfig)

    loginAsUser(testData.user.username)

    modifyTranslationData(testData.projectBuilder.self.id, "cs", testData.key2.name)
    mockedSlackClient.chatPostMessageRequests.assert.hasSize(0)
    slackMessageService.find(testData.key2.id, config.id).forEach {
      it.languageTags.assert.doesNotContain("cs")
    }
  }

  @Test
  fun `Doesn't send a message if the event isn't in subscribed by user`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val mockedSlackClient = mockSlackClient()

    val updatedConfig =
      SlackConfigDto(
        project = testData.projectBuilder.self,
        slackId = "testSlackId",
        channelId = "testChannel",
        userAccount = testData.user,
        languageTag = "en",
        events = mutableSetOf(EventName.TRANSLATION_CHANGED),
        slackTeamId = "",
      )
    slackConfigService.delete(testData.slackConfig.project.id, "testChannel", "")
    val config = slackConfigService.createOrUpdate(updatedConfig)

    loginAsUser(testData.user.username)

    addKeyToProject(testData.projectBuilder.self.id)
    mockedSlackClient.chatPostMessageRequests.assert.hasSize(0)
    slackMessageService.find(testData.key2.id, config.id).forEach {
      it.languageTags.assert.doesNotContain("en")
    }
  }

  fun mockSlackClient(): MockedSlackClient {
    val methodsClientMock = mock<MethodsClient>()
    whenever(slackClient.methods(any())).thenReturn(methodsClientMock)
    val mockPostMessageResponse = mock<ChatPostMessageResponse>()
    whenever(mockPostMessageResponse.isOk).thenReturn(true)
    whenever(mockPostMessageResponse.ts).thenReturn("ts")

    val mockUsersResponse = mock<UsersInfoResponse>()
    whenever(mockUsersResponse.isOk).thenReturn(true)

    whenever(
      methodsClientMock.chatPostMessage(
        any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>(),
      ),
    ).thenReturn(mockPostMessageResponse)

    whenever(
      methodsClientMock.usersInfo(
        any<RequestConfigurator<UsersInfoRequest.UsersInfoRequestBuilder>>(),
      ),
    ).thenReturn(mockUsersResponse)

    return MockedSlackClient(methodsClientMock)
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
      "/v2/projects/$projectId/translations",
      mapOf(
        "key" to "newKey",
        "translations" to mapOf("en" to "Sample Translation"),
      ),
    ).andIsOk
  }
}
