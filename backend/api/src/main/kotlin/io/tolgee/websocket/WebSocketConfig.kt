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
        ): Message<*>? {
          val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

          if (accessor?.command == StompCommand.CONNECT) {
            accessor.user = websocketAuthenticationResolver.resolve(accessor)
          }

          if (accessor?.command != StompCommand.SUBSCRIBE) {
            return message
          }

          val authentication = accessor.user as? TolgeeAuthentication
          return when (decideSubscribe(authentication, accessor.destination)) {
            SubscribeDecision.DENY -> null
            // Throwing here makes Spring close the whole WebSocket, not just reject the SUBSCRIBE.
            SubscribeDecision.UNAUTHENTICATED -> throw MessagingException("Unauthenticated")
            SubscribeDecision.ALLOW -> message
          }
        }
      },
    )
  }

  private enum class SubscribeDecision {
    ALLOW,
    DENY,
    UNAUTHENTICATED,
  }

  private fun decideSubscribe(
    authentication: TolgeeAuthentication?,
    destination: String?,
  ): SubscribeDecision {
    val projectId = topicId(PROJECT_TOPIC, destination)
    if (projectId != null) {
      val auth = authentication ?: return SubscribeDecision.UNAUTHENTICATED
      return decision(isProjectSubscribeAllowed(auth, projectId), destination)
    }

    val userId = topicId(USER_TOPIC, destination)
    if (userId != null) {
      val auth = authentication ?: return SubscribeDecision.UNAUTHENTICATED
      return decision(isUserSubscribeAllowed(auth, userId), destination)
    }

    logger().debug("Denying websocket subscription to unrecognized destination {}", destination)
    return SubscribeDecision.DENY
  }

  private fun topicId(
    topic: Regex,
    destination: String?,
  ): Long? =
    topic
      .find(destination.orEmpty())
      ?.groupValues
      ?.get(1)
      // toLongOrNull, not toLong: an overflow id must fall through to deny, not throw (which closes the connection).
      ?.toLongOrNull()

  private fun decision(
    allowed: Boolean,
    destination: String?,
  ): SubscribeDecision {
    if (allowed) return SubscribeDecision.ALLOW
    logger().debug("Denying websocket subscription to {}", destination)
    return SubscribeDecision.DENY
  }

  private fun isProjectSubscribeAllowed(
    authentication: TolgeeAuthentication,
    projectId: Long,
  ): Boolean {
    try {
      securityService.checkProjectPermission(
        projectId = projectId,
        requiredPermission = Scope.KEYS_VIEW,
        user = authentication.principal,
        apiKey = authentication.credentials as? ApiKeyDto,
      )
    } catch (e: PermissionException) {
      logger().debug("User / API key does not have required scopes", e)
      return false
    }
    return true
  }

  private fun isUserSubscribeAllowed(
    authentication: TolgeeAuthentication,
    userId: Long,
  ): Boolean {
    if (authentication.credentials is ApiKeyDto) {
      return false
    }
    return authentication.principal.id == userId
  }

  private companion object {
    val PROJECT_TOPIC = "^/projects/([0-9]+)/".toRegex()
    val USER_TOPIC = "^/users/([0-9]+)/".toRegex()
  }
}
