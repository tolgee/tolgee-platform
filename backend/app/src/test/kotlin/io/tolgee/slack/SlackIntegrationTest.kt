package io.tolgee.slack

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import io.tolgee.AbstractSpringTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class SlackIntegrationTest : AbstractSpringTest() {
  @Autowired
  @MockBean
  lateinit var slackClient: Slack

  @Test
  fun `it works`() {
    val methodsClientMock = mock<MethodsClient>()
    whenever(slackClient.methods(any())).thenReturn(methodsClientMock)

    slackClient.methods("ahhs").chatPostMessage {
      it.channel("channel")
      it.text("text")
    }

    val details = Mockito.mockingDetails(slackClient)
    details
  }
}
