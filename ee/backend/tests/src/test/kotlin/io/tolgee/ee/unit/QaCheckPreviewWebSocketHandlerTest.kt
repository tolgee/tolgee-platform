package io.tolgee.ee.unit

import io.tolgee.ee.api.v2.controllers.qa.QaCheckPreviewWebSocketHandler
import io.tolgee.ee.data.qa.QaPreviewWsSessionState
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.model.enums.qa.QaCheckType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import tools.jackson.databind.ObjectMapper
import java.io.IOException

class QaCheckPreviewWebSocketHandlerTest {
  private val qaCheckRunnerService =
    mock<QaCheckRunnerService> {
      wheneverBlocking { it.runCheckWithDebounce(any(), any()) }.thenReturn(emptyList())
    }

  private val handler =
    QaCheckPreviewWebSocketHandler(
      objectMapper = ObjectMapper(),
      projectQaConfigService = mock(),
      languageService = mock(),
      translationService = mock(),
      qaIssueService = mock(),
      qaCheckRunnerService = qaCheckRunnerService,
      jwtService = mock(),
      securityService = mock(),
      projectFeatureGuard = mock(),
      projectService = mock(),
      keyService = mock(),
      glossaryTermService = mock(),
      enabledFeaturesProvider = mock(),
    )

  private val enabledCheckTypes = listOf(QaCheckType.EMPTY_TRANSLATION, QaCheckType.TRIM_CHECK)

  private val state =
    QaPreviewWsSessionState(
      projectId = 1L,
      baseText = null,
      baseLanguageTag = null,
      languageTag = "cs",
      keyId = null,
      translationId = null,
      enabledCheckTypes = enabledCheckTypes,
      isPlural = false,
      baseVariants = null,
      maxCharLimit = null,
      icuPlaceholders = true,
      organizationOwnerId = 1L,
      glossaryEnabled = false,
    )

  private val oneResultPerCheckPlusDone = enabledCheckTypes.size + 1

  @Test
  fun `runChecks survives a session that closes mid-send`() {
    val session = closingSession(IllegalStateException("The remote endpoint was in state [TEXT_PARTIAL_WRITING]"))

    runBlocking { handler.runChecks(session, state, "text") }

    verify(session, times(oneResultPerCheckPlusDone)).sendMessage(any<TextMessage>())
    verify(session, times(oneResultPerCheckPlusDone)).close(CloseStatus.SESSION_NOT_RELIABLE)
  }

  @Test
  fun `runChecks survives an IO failure on send`() {
    val session = closingSession(IOException("Broken pipe"))

    runBlocking { handler.runChecks(session, state, "text") }

    verify(session, times(oneResultPerCheckPlusDone)).sendMessage(any<TextMessage>())
    verify(session, times(oneResultPerCheckPlusDone)).close(CloseStatus.SESSION_NOT_RELIABLE)
  }

  private fun closingSession(failure: Exception): WebSocketSession {
    val session = mock<WebSocketSession>()
    whenever(session.isOpen).thenReturn(true)
    whenever(session.id).thenReturn("s1")
    doThrow(failure).whenever(session).sendMessage(any<TextMessage>())
    return session
  }
}
