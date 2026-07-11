package io.tolgee.ee.slack

import com.slack.api.RequestConfigurator
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostEphemeralRequest
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.chat.ChatUpdateRequest
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.methods.response.chat.ChatPostEphemeralResponse
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.chat.ChatUpdateResponse
import com.slack.api.methods.response.users.UsersInfoResponse
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MockedSlackClient(
  val methodsClientMock: MethodsClient,
) {
  val chatPostMessageRequests: List<ChatPostMessageRequest>
    get() =
      Mockito.mockingDetails(methodsClientMock).invocations.mapNotNull { invocation ->
        if (invocation.method.name != "chatPostMessage") {
          return@mapNotNull null
        }

        val argument = invocation.arguments.singleOrNull()

        @Suppress("UNCHECKED_CAST")
        val configurator =
          argument as? RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>
            ?: return@mapNotNull null
        configurator.configure(ChatPostMessageRequest.builder()).build()
      }

  val chatUpdateRequests: List<ChatUpdateRequest>
    get() =
      Mockito.mockingDetails(methodsClientMock).invocations.mapNotNull { invocation ->
        if (invocation.method.name != "chatUpdate") {
          return@mapNotNull null
        }

        val argument = invocation.arguments.singleOrNull()

        @Suppress("UNCHECKED_CAST")
        val configurator =
          argument as? RequestConfigurator<ChatUpdateRequest.ChatUpdateRequestBuilder>
            ?: return@mapNotNull null
        configurator.configure(ChatUpdateRequest.builder()).build()
      }

  fun clearInvocations() {
    Mockito.clearInvocations(methodsClientMock)
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
        methodsClientMock.chatPostEphemeral(
          any<RequestConfigurator<ChatPostEphemeralRequest.ChatPostEphemeralRequestBuilder>>(),
        ),
      ).thenReturn(
        ChatPostEphemeralResponse().also {
          it.isOk = true
        },
      )

      whenever(
        methodsClientMock.usersInfo(
          any<RequestConfigurator<UsersInfoRequest.UsersInfoRequestBuilder>>(),
        ),
      ).thenReturn(mockUsersResponse)

      whenever(
        methodsClientMock.chatUpdate(any<RequestConfigurator<ChatUpdateRequest.ChatUpdateRequestBuilder>>()),
      ).thenReturn(
        ChatUpdateResponse().also { it.isOk = true },
      )

      return MockedSlackClient(methodsClientMock)
    }
  }
}
