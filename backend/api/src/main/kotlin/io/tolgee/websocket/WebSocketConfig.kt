package io.tolgee.websocket

import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.JwtService
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
  private val jwtService: JwtService,
  @Lazy
  private val securityService: SecurityService,
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
            val tokenString = accessor.getNativeHeader("jwtToken")?.firstOrNull()
            accessor.user = if (tokenString == null) null else jwtService.validateToken(tokenString)
          }

          if (accessor?.command == StompCommand.SUBSCRIBE) {
            val projectId =
              accessor.destination?.let {
                "/projects/([0-9]+)".toRegex().find(it)?.groupValues
                  ?.getOrNull(1)?.toLong()
              }

            if (projectId != null) {
              val user =
                (accessor.user as? TolgeeAuthentication)?.principal
                  ?: throw MessagingException("Unauthenticated")

              try {
                securityService.checkProjectPermissionNoApiKey(projectId = projectId, Scope.KEYS_VIEW, user)
              } catch (e: Exception) {
                throw MessagingException("Forbidden")
              }
            }
          }

          return message
        }
      },
    )
  }
}
