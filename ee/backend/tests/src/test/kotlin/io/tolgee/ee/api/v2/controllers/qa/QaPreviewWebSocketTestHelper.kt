package io.tolgee.ee.api.v2.controllers.qa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.fixtures.WaitNotSatisfiedException
import io.tolgee.fixtures.waitFor
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

/**
 * Test helper for the raw (non-STOMP) QA preview WebSocket endpoint.
 *
 * Usage:
 * ```
 * val ws = QaPreviewWebSocketTestHelper(port)
 * ws.connect()
 * ws.sendInit(token, projectId, keyId, languageTag)
 * ws.sendText("Hello world")
 * ws.waitForDone()
 * val results = ws.collectResults()
 * ws.close()
 * ```
 */
class QaPreviewWebSocketTestHelper(
  private val port: Int,
  private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) {
  private var session: WebSocketSession? = null
  val receivedMessages = LinkedBlockingDeque<Map<String, Any?>>()
  var closeStatus: org.springframework.web.socket.CloseStatus? = null
    private set

  fun connect() {
    val client = StandardWebSocketClient()
    val uri = URI("ws://localhost:$port/ws/qa-preview")
    session =
      client
        .execute(
          object : TextWebSocketHandler() {
            override fun handleTextMessage(
              session: WebSocketSession,
              message: TextMessage,
            ) {
              @Suppress("UNCHECKED_CAST")
              val parsed = objectMapper.readValue(message.payload, Map::class.java) as Map<String, Any?>
              receivedMessages.add(parsed)
            }

            override fun afterConnectionClosed(
              session: WebSocketSession,
              status: org.springframework.web.socket.CloseStatus,
            ) {
              closeStatus = status
            }
          },
          WebSocketHttpHeaders(),
          uri,
        ).get(10, TimeUnit.SECONDS)
  }

  fun sendInit(
    token: String,
    projectId: Long,
    keyId: Long?,
    languageTag: String,
  ) {
    val msg =
      mutableMapOf<String, Any?>(
        "token" to token,
        "projectId" to projectId,
        "languageTag" to languageTag,
      )
    if (keyId != null) {
      msg["keyId"] = keyId
    }
    send(msg)
  }

  fun sendText(text: String) {
    send(mapOf("text" to text))
  }

  private fun send(payload: Any) {
    session!!.sendMessage(TextMessage(objectMapper.writeValueAsString(payload)))
  }

  /**
   * Waits until a "done" message is received.
   */
  fun waitForDone(timeout: Long = 10000) {
    waitFor(timeout) {
      receivedMessages.any { it["type"] == "done" }
    }
  }

  /**
   * Waits until the connection is closed by the server.
   */
  fun waitForClose(timeout: Long = 5000) {
    waitFor(timeout) {
      closeStatus != null
    }
  }

  /**
   * Returns all "result" messages received so far, each containing "checkType" and "issues".
   */
  fun collectResults(): List<Map<String, Any?>> {
    return receivedMessages.filter { it["type"] == "result" }
  }

  /**
   * Returns all issues from all "result" messages, flattened.
   */
  fun collectAllIssues(): List<Map<String, Any?>> {
    return collectResults().flatMap {
      @Suppress("UNCHECKED_CAST")
      (it["issues"] as? List<Map<String, Any?>>) ?: emptyList()
    }
  }

  /**
   * Returns the first "error" message, if any.
   */
  fun getError(): Map<String, Any?>? {
    return receivedMessages.firstOrNull { it["type"] == "error" }
  }

  fun close() {
    try {
      session?.close()
    } catch (_: Exception) {
    }
  }
}
