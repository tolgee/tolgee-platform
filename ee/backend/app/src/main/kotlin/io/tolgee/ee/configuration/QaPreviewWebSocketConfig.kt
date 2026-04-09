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
    // Permissive CORS is intentional — the WebSocket requires JWT authentication
    // in the init message, so cross-origin connections without a valid token are rejected.
    registry
      .addHandler(handler, "/ws/qa-preview")
      .setAllowedOriginPatterns("*")
  }
}
