package io.tolgee.ee.component.llm

import io.sentry.Sentry
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
class OllamaApiService :
  AbstractLlmApiService(),
  Logging {
  override fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")

    val messages = getMessages(params)

    val requestBody =
      RequestBody(
        model = config.model!!,
        messages = messages,
        format = if (params.shouldOutputJson && config.format == "json") "json" else null,
      )

    val request = HttpEntity(requestBody, headers)

    val response =
      restTemplate.exchange<ResponseBody>(
        "${config.apiUrl}/api/chat",
        HttpMethod.POST,
        request,
      )

    setSentryContext(request, response)

    return PromptResult(
      response.body?.message?.content
        ?: throw LlmEmptyResponseException(),
      usage = null,
    )
  }

  fun getMessages(params: LlmParams): MutableList<RequestMessage> {
    val messages = mutableListOf<RequestMessage>()

    params.messages.forEach {
      if (it.type == LlmParams.Companion.LlmMessageType.TEXT && it.text != null) {
        messages.add(RequestMessage(role = "user", content = it.text))
      } else if (it.type == LlmParams.Companion.LlmMessageType.IMAGE && it.image != null) {
        messages.add(
          RequestMessage(
            role = "user",
            images = listOf("${it.image}"),
          ),
        )
      }
    }

    if (params.shouldOutputJson) {
      messages.add(
        RequestMessage(role = "user", content = "Strictly return only valid json!"),
      )
    }
    return messages
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    class RequestBody(
      val model: String,
      val messages: List<RequestMessage>,
      val stream: Boolean = false,
      val format: String? = "json",
    )

    class RequestMessage(
      val role: String,
      val content: String? = null,
      val images: List<String>? = null,
    )

    class ResponseBody(
      val model: String,
      val message: ResponseMessage,
    )

    class ResponseMessage(
      val role: String,
      val content: String,
    )
  }
}
