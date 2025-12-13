package io.tolgee.websocket

import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.SecurityService
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
            val authorization = accessor.getNativeHeader("authorization")?.firstOrNull()
            val xApiKey = accessor.getNativeHeader("x-api-key")?.firstOrNull()
            val legacyJwt = accessor.getNativeHeader("jwtToken")?.firstOrNull()
            accessor.user = websocketAuthenticationResolver.resolve(authorization, xApiKey, legacyJwt)
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

    val creds = authentication.credentials
    if (creds is ApiKeyDto) {
      val matchesProject = creds.projectId == projectId
      val hasScope = creds.scopes.contains(Scope.KEYS_VIEW)
      if (!matchesProject || !hasScope) {
        throw MessagingException("Forbidden")
      }
      return
    }

    val user = authentication.principal
    try {
      securityService.checkProjectPermissionNoApiKey(projectId = projectId, Scope.KEYS_VIEW, user)
    } catch (e: Exception) {
      throw MessagingException("Forbidden")
    }
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
      throw MessagingException("Forbidden")
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
}
