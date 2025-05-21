package io.tolgee.ee.component.llm

import com.fasterxml.jackson.annotation.JsonInclude
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.dtos.LLMParams
import io.tolgee.dtos.response.prompt.PromptResponseUsageDto
import io.tolgee.service.PromptService
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
import java.util.*

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class GeminiApiService : AbstractLLMApiService(), Logging {
  override fun translate(
    params: LLMParams,
    config: LLMProviderInterface,
    restTemplate: RestTemplate,
  ): PromptService.Companion.PromptResult {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("api-key", config.apiKey)

    val inputMessages = params.messages.toMutableList()

    if (params.shouldOutputJson) {
      inputMessages.add(
        LLMParams.Companion.LlmMessage(LLMParams.Companion.LlmMessageType.TEXT, "Return only valid json!"),
      )
    }

    val contents = mutableListOf<RequestContent>()

    inputMessages.forEach {
      if (it.type == LLMParams.Companion.LlmMessageType.TEXT && it.text != null) {
        contents.add(RequestContent(parts = listOf(RequestPart(text = it.text!!))))
      } else if (it.type == LLMParams.Companion.LlmMessageType.IMAGE && it.image != null) {
        contents.add(
          RequestContent(
            parts =
              listOf(
                RequestPart(
                  inlineData =
                    RequestInlineData(
                      mimeType = "image/png",
                      data = Base64.getEncoder().encodeToString(it.image),
                    ),
                ),
              ),
          ),
        )
      }
    }

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

    return PromptService.Companion.PromptResult(
      response =
        response.body?.candidates?.first()?.content?.parts?.first()?.text
          ?: throw RuntimeException(response.toString()),
      usage =
        response.body?.usageMetadata?.let {
          PromptResponseUsageDto(
            inputTokens = it.promptTokenCount,
            outputTokens = it.candidatesTokenCount,
          )
        },
    )
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
      val totalTokenCount: Long,
    )
  }
}
