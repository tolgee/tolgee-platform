package io.tolgee.ee.component.llm

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
class AnthropicApiService :
  AbstractLlmApiService(),
  Logging {
  override fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult {
    val headers = getHeaders(config)

    val messages = getMessages(params)

    val requestBody =
      RequestBody(
        messages = messages,
        model = config.model,
        max_tokens = config.maxTokens,
      )

    val request = HttpEntity(requestBody, headers)

    val response: ResponseEntity<ResponseBody> =
      restTemplate.exchange<ResponseBody>(
        "${config.apiUrl}/v1/messages",
        HttpMethod.POST,
        request,
      )

    setSentryContext(request, response)

    return PromptResult(
      response.body
        ?.content
        ?.firstOrNull()
        ?.text
        ?: throw LlmEmptyResponseException(),
      usage =
        response.body?.usage?.let {
          PromptResult.Usage(
            inputTokens = it.input_tokens,
            outputTokens = it.output_tokens,
          )
        },
    )
  }

  private fun getHeaders(config: LlmProviderInterface): HttpHeaders {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("anthropic-version", "2023-06-01")
    headers.set("x-api-key", config.apiKey)
    return headers
  }

  private fun getMessages(params: LlmParams): MutableList<RequestMessage> {
    val inputMessages = params.messages.toMutableList()

    val messages = mutableListOf<RequestMessage>()

    inputMessages.forEach {
      if (it.type == LlmParams.Companion.LlmMessageType.TEXT && it.text != null) {
        messages.add(RequestMessage(role = "user", content = it.text!!))
      } else if (it.type == LlmParams.Companion.LlmMessageType.IMAGE && it.image != null) {
        messages.add(
          RequestMessage(
            role = "user",
            content =
              listOf(
                RequestMessageContent(
                  type = "image",
                  source =
                    RequestImage(
                      data = it.image!!,
                    ),
                ),
              ),
          ),
        )
      }
    }

    if (params.shouldOutputJson) {
      messages.add(
        RequestMessage(
          role = "user",
          content = "Return valid JSON and only JSON! Output message is parsed by machine!",
        ),
      )
    }
    return messages
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    @Suppress("unused")
    class RequestBody(
      val max_tokens: Long = 1000,
      val stream: Boolean = false,
      val messages: List<RequestMessage>,
      val model: String?,
      val temperature: Long? = 0,
    )

    class RequestMessage(
      val role: String,
      val content: Any,
    )

    class RequestMessageContent(
      val type: String,
      val source: RequestImage,
    )

    class RequestImage(
      val type: String = "base64",
      val media_type: String = "image/png",
      val data: String,
    )

    class ResponseBody(
      val content: List<ResponseMessage>,
      val usage: ResponseUsage,
    )

    class ResponseMessage(
      val text: String,
    )

    class ResponseUsage(
      val input_tokens: Long,
      val output_tokens: Long,
    )
  }
}
