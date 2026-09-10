package io.tolgee.websocket

import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.websocket.WebsocketSubscribeAuthorizer.SubscribeDecision
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
import java.security.Principal

/** Websocket authentication and per-topic authorization model: docs/websocket/README.md. */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
  @Lazy
  private val websocketAuthenticationResolver: WebsocketAuthenticationResolver,
  @Lazy
  private val subscribeAuthorizer: WebsocketSubscribeAuthorizer,
) : WebSocketMessageBrokerConfigurer,
  Logging {
  override fun configureMessageBroker(config: MessageBrokerRegistry) {
    config.enableSimpleBroker("/")
  }

  override fun registerStompEndpoints(registry: StompEndpointRegistry) {
    registry
      .addEndpoint("/websocket")
      .setAllowedOriginPatterns("*")
      .withSockJS()
  }

  override fun configureClientInboundChannel(registration: ChannelRegistration) {
    registration.interceptors(
      object : ChannelInterceptor {
        override fun preSend(
          message: Message<*>,
          channel: MessageChannel,
        ): Message<*>? {
          val accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message

          return when (accessor.command) {
            // STOMP 1.2 lets a client open with either command.
            StompCommand.CONNECT, StompCommand.STOMP -> {
              val resolved = websocketAuthenticationResolver.resolve(accessor)
              rememberResolvedAuthentication(accessor, resolved)
              accessor.user = resolved ?: Principal { "unauthenticated-${accessor.sessionId}" }
              message
            }

            StompCommand.SEND, StompCommand.MESSAGE -> {
              logger.debug("Denying client {} to {}", accessor.command, accessor.destination)
              null
            }

            StompCommand.SUBSCRIBE -> {
              when (subscribeAuthorizer.decide(resolvedAuthentication(accessor), accessor.destination)) {
                SubscribeDecision.DENY -> null
                SubscribeDecision.UNAUTHENTICATED -> throw MessagingException("Unauthenticated")
                SubscribeDecision.ALLOW -> message
              }
            }

            else -> message
          }
        }
      },
    )
  }

  private fun rememberResolvedAuthentication(
    accessor: StompHeaderAccessor,
    resolved: TolgeeAuthentication?,
  ) {
    val attributes = accessor.sessionAttributes
    if (attributes == null) {
      logger.warn("No STOMP session attributes; the resolved authentication cannot be remembered")
      return
    }
    if (resolved == null) {
      attributes.remove(RESOLVED_AUTHENTICATION_ATTRIBUTE)
      return
    }
    attributes[RESOLVED_AUTHENTICATION_ATTRIBUTE] = resolved
  }

  private fun resolvedAuthentication(accessor: StompHeaderAccessor): TolgeeAuthentication? =
    accessor.sessionAttributes?.get(RESOLVED_AUTHENTICATION_ATTRIBUTE) as? TolgeeAuthentication

  private companion object {
    const val RESOLVED_AUTHENTICATION_ATTRIBUTE = "io.tolgee.resolvedAuthentication"
  }
}
