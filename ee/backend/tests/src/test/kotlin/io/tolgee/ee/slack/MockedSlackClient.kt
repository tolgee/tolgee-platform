package io.tolgee.ee.slack

import com.slack.api.RequestConfigurator
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import org.mockito.Mockito

class MockedSlackClient(val methodsClientMock: MethodsClient) {
  val chatPostMessageRequests: List<ChatPostMessageRequest>
    get() =
      Mockito.mockingDetails(methodsClientMock).invocations.flatMap { invocation ->
        invocation.arguments.mapNotNull { argument ->
          try {
            @Suppress("UNCHECKED_CAST")
            val configurator = argument as RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>
            configurator.configure(ChatPostMessageRequest.builder()).build()
          } catch (e: Exception) {
            null
          }
        }
      }
}
