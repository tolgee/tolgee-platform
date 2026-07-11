package io.tolgee.ee.component.llm

import com.fasterxml.jackson.annotation.JsonInclude
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import io.tolgee.exceptions.LlmEmptyResponseException
import io.tolgee.util.Logging
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class GoogleAiApiService :
  AbstractLlmApiService(),
  Logging {
  override fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult {
    val headers = getHeaders(config)
    val contents = getContents(params)

    val requestBody =
      RequestBody(
        contents = contents,
        generationConfig =
          RequestGenerationConfig(
            responseMimeType = if (params.shouldOutputJson) "application/json" else null,
          ),
      )

    val request = HttpEntity(requestBody, headers)

    val response: ResponseEntity<ResponseBody> =
      restTemplate.exchange<ResponseBody>(
        "${config.apiUrl}/v1beta/models/${config.model}:generateContent?key=${config.apiKey}",
        HttpMethod.POST,
        request,
      )

    setSentryContext(request, response)

    return PromptResult(
      response =
        response.body
          ?.candidates
          ?.firstOrNull()
          ?.content
          ?.parts
          ?.firstOrNull()
          ?.text
          ?: throw LlmEmptyResponseException(),
      usage =
        response.body?.usageMetadata?.let {
          PromptResult.Usage(
            inputTokens = it.promptTokenCount,
            outputTokens = it.candidatesTokenCount,
          )
        },
    )
  }

  fun getContents(params: LlmParams): MutableList<RequestContent> {
    val contents = mutableListOf<RequestContent>()

    params.messages.forEach {
      if (it.type == LlmParams.Companion.LlmMessageType.TEXT && it.text != null) {
        contents.add(RequestContent(parts = listOf(RequestPart(text = it.text!!))))
      } else if (it.type == LlmParams.Companion.LlmMessageType.IMAGE && it.image != null) {
        contents.add(
          RequestContent(
            parts =
              listOf(
                RequestPart(
                  inlineData =
                    RequestInlineData(
                      mimeType = "image/png",
                      data = it.image!!,
                    ),
                ),
              ),
          ),
        )
      }
    }

    if (params.shouldOutputJson) {
      contents.add(
        RequestContent(parts = listOf(RequestPart(text = "Strictly return only valid json!"))),
      )
    }

    return contents
  }

  fun getHeaders(config: LlmProviderInterface): HttpHeaders {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("api-key", config.apiKey)
    return headers
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    @Suppress("unused")
    class RequestBody(
      val contents: List<RequestContent>,
      val generationConfig: RequestGenerationConfig?,
    )

    class RequestContent(
      val role: String = "user",
      val parts: List<RequestPart>,
    )

    class RequestGenerationConfig(
      val responseMimeType: String? = null,
    )

    class RequestPart(
      @JsonInclude(JsonInclude.Include.NON_NULL) val text: String? = null,
      @JsonInclude(JsonInclude.Include.NON_NULL) val inlineData: RequestInlineData? = null,
    )

    class RequestInlineData(
      val mimeType: String,
      val data: String,
    )

    class ResponseBody(
      val candidates: List<ResponseCandidate>,
      val usageMetadata: ResponseUsageMetadata,
    )

    class ResponseCandidate(
      val content: ResponseContent,
    )

    class ResponseContent(
      val parts: List<ResponsePart>,
    )

    class ResponsePart(
      val text: String?,
    )

    class ResponseUsageMetadata(
      val promptTokenCount: Long,
      val candidatesTokenCount: Long,
    )
  }
}
