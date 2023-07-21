package io.tolgee.websocket

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.enums.Scope
import io.tolgee.security.JwtTokenProvider
import io.tolgee.service.security.SecurityService
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
  private val jwtTokenProvider: JwtTokenProvider,
  private val securityService: SecurityService,
) : WebSocketMessageBrokerConfigurer {
  override fun configureMessageBroker(config: MessageBrokerRegistry) {
    config.enableSimpleBroker("/")
  }

  override fun registerStompEndpoints(registry: StompEndpointRegistry) {
    registry.addEndpoint("/websocket").setAllowedOriginPatterns("*").withSockJS()
  }

  override fun configureClientInboundChannel(registration: ChannelRegistration) {
    registration.interceptors(object : ChannelInterceptor {
      override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor?.command == StompCommand.CONNECT) {
          val tokenString = accessor.getNativeHeader("jwtToken")?.firstOrNull()
          accessor.user = jwtTokenProvider.getAuthentication(tokenString)
        }

        if (accessor?.command == StompCommand.SUBSCRIBE) {
          val projectId = accessor.destination?.let {
            "/projects/([0-9]+)".toRegex().find(it)?.groupValues
              ?.getOrNull(1)?.toLong()
          }

          if (projectId != null) {
            try {
              val user = (accessor.user as? UsernamePasswordAuthenticationToken)?.principal as UserAccountDto
              securityService.checkProjectPermissionNoApiKey(projectId = projectId, Scope.TRANSLATIONS_VIEW, user)
            } catch (e: Exception) {
              throw MessagingException("Forbidden")
            }
          }
        }

        return message
      }

      override fun postReceive(message: Message<*>, channel: MessageChannel): Message<*>? {
        return super.postReceive(message, channel)
      }
    })
  }
}
