package io.tolgee.websocket

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.support.AbstractSubscribableChannel
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler

/**
 * Takes the place of `@EnableWebSocketMessageBroker` (which only imports the parent class) so the session
 * decorator can be swapped for [ErrorFrameFlushingSessionDecorator]. Putting the annotation back drops that fix.
 */
@Configuration(proxyBeanMethods = false)
class WebSocketBrokerConfiguration : DelegatingWebSocketMessageBrokerConfiguration() {
  @Bean
  override fun subProtocolWebSocketHandler(
    clientInboundChannel: AbstractSubscribableChannel,
    clientOutboundChannel: AbstractSubscribableChannel,
  ): WebSocketHandler =
    object : SubProtocolWebSocketHandler(clientInboundChannel, clientOutboundChannel) {
      override fun decorateSession(session: WebSocketSession): WebSocketSession =
        ErrorFrameFlushingSessionDecorator(session, sendTimeLimit, sendBufferSizeLimit)
    }.also { it.phase = phase }
}
