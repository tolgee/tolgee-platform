package io.tolgee.websocket

import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.SecurityService
import io.tolgee.util.logger
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
  @Lazy
  private val securityService: SecurityService,
  @Lazy
  private val websocketAuthenticationResolver: WebsocketAuthenticationResolver,
) : WebSocketMessageBrokerConfigurer {
  override fun configureMessageBroker(config: MessageBrokerRegistry) {
    config.enableSimpleBroker("/")
  }

  override fun registerStompEndpoints(registry: StompEndpointRegistry) {
    registry.addEndpoint("/websocket").setAllowedOriginPatterns("*").withSockJS()
  }

  override fun configureClientInboundChannel(registration: ChannelRegistration) {
    registration.interceptors(
      object : ChannelInterceptor {
        override fun preSend(
          message: Message<*>,
          channel: MessageChannel,
        ): Message<*> {
          val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

          if (accessor?.command == StompCommand.CONNECT) {
            accessor.user = websocketAuthenticationResolver.resolve(accessor)
          }

          val authentication = accessor?.user as? TolgeeAuthentication

          if (accessor?.command == StompCommand.SUBSCRIBE) {
            checkProjectPathPermissionsAuth(authentication, accessor.destination)
            checkUserPathPermissionsAuth(authentication, accessor.destination)
          }

          return message
        }
      },
    )
  }

  fun checkProjectPathPermissionsAuth(
    authentication: TolgeeAuthentication?,
    destination: String?,
  ) {
    val projectId =
      destination?.let {
        "/projects/([0-9]+)"
          .toRegex()
          .find(it)
          ?.groupValues
          ?.getOrNull(1)
          ?.toLong()
      } ?: return

    if (authentication == null) {
      throw MessagingException("Unauthenticated")
    }

    val apiKey = authentication.credentials as? ApiKeyDto
    val user = authentication.principal

    try {
      securityService.checkProjectPermission(
        projectId = projectId,
        requiredPermission = Scope.KEYS_VIEW,
        user = user,
        apiKey = apiKey,
      )
    } catch (e: PermissionException) {
      logger().debug("User / API key does not have required scopes", e)
      throwForbidden()
    }

    return
  }

  fun checkUserPathPermissionsAuth(
    authentication: TolgeeAuthentication?,
    destination: String?,
  ) {
    val userId =
      destination?.let {
        "/users/([0-9]+)"
          .toRegex()
          .find(it)
          ?.groupValues
          ?.getOrNull(1)
          ?.toLong()
      } ?: return

    if (authentication == null) {
      throw MessagingException("Unauthenticated")
    }

    val creds = authentication.credentials
    if (creds is ApiKeyDto) {
      // API keys must not subscribe to user topics
      throw MessagingException("Forbidden")
    }

    val user = (authentication as? TolgeeAuthentication)?.principal
    if (user?.id != userId) {
      throw MessagingException("Forbidden")
    }
  }

  private fun throwForbidden() {
    throw MessagingException("Forbidden")
  }
}
