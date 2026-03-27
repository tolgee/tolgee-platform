package io.tolgee.ee.api.v2.controllers.qa

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.ee.data.qa.QaCheckPreviewDone
import io.tolgee.ee.data.qa.QaCheckPreviewError
import io.tolgee.ee.data.qa.QaCheckPreviewResult
import io.tolgee.ee.data.qa.QaPreviewWsIssue
import io.tolgee.ee.data.qa.QaPreviewWsSessionState
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.formats.getPluralForms
import io.tolgee.model.enums.Scope
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.security.authentication.JwtService
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class QaCheckPreviewWebSocketHandler(
  private val objectMapper: ObjectMapper,
  private val projectQaConfigService: ProjectQaConfigService,
  private val languageService: LanguageService,
  private val translationService: TranslationService,
  private val qaIssueService: QaIssueService,
  private val qaCheckRunnerService: QaCheckRunnerService,
  private val jwtService: JwtService,
  private val securityService: SecurityService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectService: ProjectService,
  private val keyService: KeyService,
) : TextWebSocketHandler() {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  suspend fun runChecks(
    session: WebSocketSession,
    state: QaPreviewWsSessionState,
    text: String,
    activeVariant: String? = null,
  ) {
    try {
      val persistedIssues = fetchPersistedIssues(state)

      val textParsed = if (state.isPlural) getPluralForms(text) else null

      val params =
        QaCheckParams(
          baseText = state.baseText,
          text = text,
          baseLanguageTag = state.baseLanguageTag,
          languageTag = state.languageTag,
          isPlural = state.isPlural,
          textVariants = textParsed?.forms,
          textVariantOffsets = textParsed?.offsets,
          baseTextVariants = state.baseVariants,
          activeVariant = activeVariant,
          maxCharLimit = state.maxCharLimit,
        )

      coroutineScope {
        state.enabledCheckTypes
          .map { check ->
            async {
              val results = qaCheckRunnerService.runCheckWithDebounce(check, params)
              val issues = results.map { QaPreviewWsIssue.fromQaCheckResult(it, persistedIssues) }
              sendMessage(session, QaCheckPreviewResult(checkType = check, issues = issues))
            }
          }.awaitAll()
      }

      sendMessage(session, QaCheckPreviewDone())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      logger.error("Error running QA checks", e)
      sendMessage(session, QaCheckPreviewDone())
    }
  }

  private fun handleTextUpdate(
    session: WebSocketSession,
    state: QaPreviewWsSessionState,
    json: JsonNode,
  ) {
    val text = json.get("text")?.asText() ?: ""
    val variant = json.get("variant")?.asText()

    state.currentJob?.cancel()
    state.currentJob = scope.launch { runChecks(session, state, text, activeVariant = variant) }
  }

  private fun handleInit(
    session: WebSocketSession,
    json: JsonNode,
  ) {
    try {
      val token =
        json.get("token")?.asText()
          ?: throw IllegalArgumentException("Missing token")
      val projectId =
        json.get("projectId")?.asLong()
          ?: throw IllegalArgumentException("Missing projectId")
      val keyId = json.get("keyId")?.asLong()
      val languageTag =
        json.get("languageTag")?.asText()
          ?: throw IllegalArgumentException("Missing languageTag")

      checkAuth(token, projectId)
      checkFeatureEnabled(projectId)
      initializeState(session, projectId, languageTag, keyId)
    } catch (e: IllegalArgumentException) {
      logger.debug("WebSocket init validation failed", e)
      sendMessage(session, QaCheckPreviewError(message = e.message ?: "Invalid request"))
      session.close(CloseStatus.POLICY_VIOLATION)
    } catch (e: Exception) {
      logger.warn("WebSocket init failed", e)
      sendMessage(session, QaCheckPreviewError(message = "Authentication failed"))
      session.close(CloseStatus.POLICY_VIOLATION)
    }
  }

  private fun checkAuth(
    token: String,
    projectId: Long,
  ): UserAccountDto {
    val auth = jwtService.validateToken(token)
    val user = auth.principal
    val apiKey = auth.credentials as? ApiKeyDto

    securityService.checkProjectPermission(
      projectId = projectId,
      requiredPermission = Scope.TRANSLATIONS_VIEW,
      user = user,
      apiKey = apiKey,
    )

    return user
  }

  private fun checkFeatureEnabled(projectId: Long) {
    val project = projectService.get(projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) {
      throw IllegalStateException("QA Checks feature is not enabled")
    }
  }

  private fun initializeState(
    session: WebSocketSession,
    projectId: Long,
    languageTag: String,
    keyId: Long?,
  ) {
    val baseLanguage = languageService.getProjectBaseLanguage(projectId)
    val baseTag: String? = baseLanguage.tag.takeIf { languageTag != it }

    val baseText: String? =
      if (baseTag != null && keyId != null) {
        translationService
          .getTranslations(listOf(keyId), listOf(baseLanguage.id))
          .firstOrNull()
          ?.text
      } else {
        null
      }

    val language = languageService.findByTag(languageTag, projectId)

    val translationId: Long? =
      if (keyId != null && language != null) {
        translationService
          .getTranslations(listOf(keyId), listOf(language.id))
          .firstOrNull()
          ?.id
      } else {
        null
      }

    val enabledCheckTypes =
      if (language != null) {
        projectQaConfigService.getEnabledCheckTypesForLanguage(projectId, language.id)
      } else {
        projectQaConfigService.getEnabledCheckTypesForProject(projectId)
      }

    val key = keyId?.let { keyService.get(it) }
    val isPlural = key?.isPlural ?: false
    val maxCharLimit = key?.maxCharLimit
    val baseParsed = if (isPlural && baseText != null) getPluralForms(baseText) else null

    session.attributes["state"] =
      QaPreviewWsSessionState(
        projectId = projectId,
        baseText = baseText,
        baseLanguageTag = baseTag,
        languageTag = languageTag,
        keyId = keyId,
        translationId = translationId,
        enabledCheckTypes = enabledCheckTypes.toList(),
        isPlural = isPlural,
        baseVariants = baseParsed?.forms,
        maxCharLimit = maxCharLimit,
      )
  }

  override fun handleTextMessage(
    session: WebSocketSession,
    message: TextMessage,
  ) {
    val json = objectMapper.readTree(message.payload)
    val state = session.attributes["state"] as? QaPreviewWsSessionState
    if (state == null) {
      handleInit(session, json)
    } else {
      handleTextUpdate(session, state, json)
    }
  }

  private fun fetchPersistedIssues(state: QaPreviewWsSessionState): List<TranslationQaIssue> {
    val translationId = state.translationId ?: return emptyList()
    return qaIssueService.getIssuesForTranslation(state.projectId, translationId)
  }

  private fun sendMessage(
    session: WebSocketSession,
    data: Any,
  ) {
    synchronized(session) {
      if (session.isOpen) {
        session.sendMessage(TextMessage(objectMapper.writeValueAsString(data)))
      }
    }
  }

  override fun afterConnectionClosed(
    session: WebSocketSession,
    status: CloseStatus,
  ) {
    val state = session.attributes["state"] as? QaPreviewWsSessionState
    state?.currentJob?.cancel()
  }

  companion object {
    private val logger = LoggerFactory.getLogger(QaCheckPreviewWebSocketHandler::class.java)
  }
}
