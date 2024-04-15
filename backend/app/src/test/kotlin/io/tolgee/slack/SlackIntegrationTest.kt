package io.tolgee.slack

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import io.tolgee.AbstractSpringTest
import io.tolgee.api.IModifiedEntityModel
import io.tolgee.api.IProjectActivityModel
import io.tolgee.component.automations.processors.slackIntegration.SavedMessageDto
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutor
import io.tolgee.component.automations.processors.slackIntegration.SlackExecutorHelper
import io.tolgee.component.automations.processors.slackIntegration.SlackRequest
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.testDataBuilder.data.SlackTestData
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.service.slackIntegration.SavedSlackMessageService
import io.tolgee.util.Logging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class SlackIntegrationTest : AbstractSpringTest(), Logging {
  @MockBean
  @Autowired
  lateinit var slackClient: Slack

  @Autowired
  lateinit var slackExecutor: SlackExecutor

  @Autowired
  override lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  @MockBean
  lateinit var savedSlackMessageService: SavedSlackMessageService

  @BeforeAll
  fun init() {
    tolgeeProperties.slack.token = "token"
  }

  @AfterEach
  fun clean() {
    reset(savedSlackMessageService)
  }

  @Test
  fun `message sent to correct channel`() {
    val methodsClientMock = mock<MethodsClient>()
    whenever(slackClient.methods(any())).thenReturn(methodsClientMock)

    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val messageDto = mock<SavedMessageDto>()

    whenever(
      methodsClientMock.chatPostMessage(
        any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>(),
      ),
    ).thenAnswer {
      val mockResponse = mock<ChatPostMessageResponse>()
      whenever(mockResponse.isOk).thenReturn(true)
      whenever(mockResponse.ts).thenReturn("123")
      mockResponse
    }

    slackExecutor.sendRegularMessageWithSaving(messageDto, testData.slackConfig)

    val builderSpy = spy(ChatPostMessageRequest.builder())
    val lambdaCaptor = argumentCaptor<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>()

    verify(methodsClientMock).chatPostMessage(lambdaCaptor.capture())
    lambdaCaptor.firstValue.configure(builderSpy)

    verify(builderSpy).channel(testData.slackConfig.channelId)

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `message sent and saved`() {
    val methodsClientMock = setUpSlackClientMocks()

    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val messageDto = mock<SavedMessageDto>()

    whenever(
      methodsClientMock.chatPostMessage(
        any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>(),
      ),
    ).thenAnswer {
      val mockResponse = mock<ChatPostMessageResponse>()
      whenever(mockResponse.isOk).thenReturn(true)
      whenever(mockResponse.ts).thenReturn("123")
      mockResponse
    }
    val message = mock<SavedSlackMessage>()
    whenever(savedSlackMessageService.create(any())).thenReturn(message)

    slackExecutor.sendRegularMessageWithSaving(messageDto, testData.slackConfig)
    verify(savedSlackMessageService, times(1)).create(any())

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `sending the message failed and the message was not saved`() {
    val methodsClientMock = setUpSlackClientMocks()

    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val messageDto = mock<SavedMessageDto>()

    whenever(
      methodsClientMock.chatPostMessage(
        any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>(),
      ),
    ).thenAnswer {
      val mockResponse = mock<ChatPostMessageResponse>()
      whenever(mockResponse.isOk).thenReturn(false)
      whenever(mockResponse.ts).thenReturn("123")
      mockResponse
    }
    val message = mock<SavedSlackMessage>()
    whenever(savedSlackMessageService.create(any())).thenReturn(message)

    slackExecutor.sendRegularMessageWithSaving(messageDto, testData.slackConfig)
    verify(savedSlackMessageService, times(0)).create(any())

    testDataService.cleanTestData(testData.root)
  }

  private fun setUpSlackClientMocks(): MethodsClient {
    val methodsClientMock = mock<MethodsClient>()
    whenever(slackClient.methods(any())).thenReturn(methodsClientMock)

    return methodsClientMock
  }

  @Test
  fun `should send one message per translation change`() {
    val testData = SlackTestData()
    testDataService.saveTestData(testData.root)
    val slackRequest = mock<SlackRequest>()
    val slackExecutorHelper = mock<SlackExecutorHelper>()

    doReturn(
      mutableListOf(
        mock<SavedSlackMessage>(),
        mock<SavedSlackMessage>(),
      ),
    ).`when`(slackExecutorHelper).createTranslationChangeMessage()

    whenever(slackExecutor.getHelper(any(), any())).thenReturn(slackExecutorHelper)
    doReturn(testData.slackConfig).`when`(slackExecutorHelper).slackConfig
    // doReturn(slackExecutorHelper).`when`(slackExecutor).getHelper(any(), any())

    val modifiedEntities = mapOf("Translation" to listOf(mock<IModifiedEntityModel>(), mock<IModifiedEntityModel>()))
    val activityData =
      mock<IProjectActivityModel>().apply {
        whenever(counts).thenReturn(mutableMapOf("Translation" to 2L)) // Ensure counts < 10 to avoid early return
      }

    whenever(slackRequest.activityData).thenReturn(activityData)
    whenever(slackExecutor.findSavedMessageOrNull(any(), any(), any())).thenReturn(emptyList())
    doNothing().whenever(slackExecutor).sendRegularMessageWithSaving(any(), any())

    slackExecutor.sendMessageOnTranslationSet(testData.slackConfig, slackRequest)

    // Verify that getHelper was indeed called
    verify(slackExecutor).getHelper(any(), any())
    // Verify that messages are sent as many times as there are translation changes
    verify(slackExecutor, times(2)).sendRegularMessageWithSaving(any(), any())

    testDataService.cleanTestData(testData.root)
  }
}
