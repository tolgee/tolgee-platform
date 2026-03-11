package io.tolgee.ee.configuration

import io.tolgee.ee.api.v2.controllers.qa.QaCheckPreviewWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class QaPreviewWebSocketConfig(
  private val handler: QaCheckPreviewWebSocketHandler,
) : WebSocketConfigurer {
  override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
    registry
      .addHandler(handler, "/ws/qa-preview")
      .setAllowedOriginPatterns("*")
  }
}
