package io.tolgee.slack

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import io.tolgee.AbstractSpringTest
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class SlackIntegrationTest : AbstractSpringTest(), Logging {
  @Autowired
  @MockBean
  lateinit var slackClient: Slack

  @Test
  fun `it mocks slack client`() {
    val mockedSlackClient = mockSlackClient()

    slackClient.methods("ahhs").chatPostMessage {
      it.channel("channel")
      it.text("text")
    }

    mockedSlackClient.chatPostMessageRequests.assert.hasSize(1)
    val request = mockedSlackClient.chatPostMessageRequests.single()
    request.channel.assert.isEqualTo("channel")
    request.text.assert.isEqualTo("text")
  }

  fun mockSlackClient(): MockedSlackClient {
    val methodsClientMock = mock<MethodsClient>()
    whenever(slackClient.methods(any())).thenReturn(methodsClientMock)
    return MockedSlackClient(methodsClientMock)
  }
}
