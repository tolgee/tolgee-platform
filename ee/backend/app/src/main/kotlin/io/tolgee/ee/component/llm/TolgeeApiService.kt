package io.tolgee.ee.component.llm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.constants.Message
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.exceptions.FailedDependencyException
import io.tolgee.util.Logging
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.client.RestTemplate

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TolgeeApiService(
  private val subscriptionService: EeSubscriptionServiceImpl,
) : AbstractLlmApiService(),
  Logging {
  override fun defaultAttempts(): List<Int> = listOf(30)

  override fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult {
    val licenseKey =
      subscriptionService.findSubscriptionDto()?.licenseKey ?: throw IllegalStateException("Not Subscribed")
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("License-Key", licenseKey)

    val request = HttpEntity(params, headers)

    val url = "${config.apiUrl}/v2/public/llm/prompt"

    val response =
      try {
        restTemplate.exchange(
          url,
          HttpMethod.POST,
          request,
          PromptResult::class.java,
        )
      } catch (e: BadRequest) {
        extractTolgeeErrorOrThrow(e)
      }

    return response?.body ?: throw FailedDependencyException(Message.LLM_PROVIDER_ERROR, listOf("Empty response body"))
  }

  fun extractTolgeeErrorOrThrow(e: BadRequest): Nothing {
    if (!e.responseBodyAsString.isNullOrBlank()) {
      val json =
        e.responseBodyAsString
          .runCatching {
            jacksonObjectMapper().readValue(e.responseBodyAsString, JsonNode::class.java)
          }.getOrNull()
      val eCode = json?.get("code")?.runCatching { this.asText() }?.getOrNull()
      val eParams = json?.get("params")?.runCatching { this.asIterable().map { it.asText() } }?.getOrNull()
      val message = eCode?.runCatching { Message.valueOf(this.uppercase()) }?.getOrNull()
      message?.let { throw FailedDependencyException(message, params = eParams, e) }
    }
    throw e
  }
}
