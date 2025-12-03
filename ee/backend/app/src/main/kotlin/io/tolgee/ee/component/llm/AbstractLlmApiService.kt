package io.tolgee.ee.component.llm

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.sentry.Sentry
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

val DEFAULT_ATTEMPTS = listOf(30)

abstract class AbstractLlmApiService {
  private val logger = LoggerFactory.getLogger(AbstractLlmApiService::class.java)

  abstract fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult

  /**
   * specify how many times and with what timeouts service should be called
   */
  open fun defaultAttempts(): List<Int> = DEFAULT_ATTEMPTS

  open fun parseErrorBody(ex: HttpClientErrorException): JsonNode? {
    val errorBody: String? = ex.responseBodyAsString
    if (!errorBody.isNullOrEmpty()) {
      val objectMapper = ObjectMapper()
      try {
        val errorResponse: JsonNode = objectMapper.readValue(errorBody)
        return errorResponse
      } catch (jsonEx: JsonProcessingException) {
        logger.debug("Failed to parse error body: ${jsonEx.message}")
        return null
      }
    } else {
      // No error body available
      logger.debug("Bad request with no error body")
      return null
    }
  }

  protected fun setSentryContext(
    request: HttpEntity<*>,
    response: ResponseEntity<*>,
  ) {
    Sentry.configureScope { scope ->
      scope.setContexts("llmDiagnosticsRequestBody", request.body)
      scope.setContexts("llmDiagnosticsResponseBody", response.body)
      scope.setContexts("llmDiagnosticsResponseHeaders", response.headers)
    }
  }
}
