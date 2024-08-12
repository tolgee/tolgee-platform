package io.tolgee.ee.slack

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.users.UsersInfoResponse
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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

  companion object {
    fun mockSlackClient(slackClient: Slack): MockedSlackClient {
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
  }
}
