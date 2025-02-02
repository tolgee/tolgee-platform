package io.tolgee.component.machineTranslation.providers

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.tolgee.configuration.tolgee.machineTranslation.OpenaiMachineTranslationProperties
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class OpenaiApiService(
  private val openaiMachineTranslationProperties: OpenaiMachineTranslationProperties,
  private val restTemplate: RestTemplate,
) {
  fun translate(
    text: String,
    sourceTag: String,
    targetTag: String,
  ): String? {
    var headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON
    headers.add("Authorization", "Bearer ${openaiMachineTranslationProperties.apiKey}")

    var prompt = openaiMachineTranslationProperties.prompt
    prompt = prompt.replace("{source}", sourceTag)
    prompt = prompt.replace("{target}", targetTag)
    prompt = prompt.replace("{text}", text)

    val requestBody = JsonObject()
    requestBody.addProperty("model", openaiMachineTranslationProperties.model)
    requestBody.add(
      "messages",
      JsonArray().apply {
        add(
          JsonObject().apply {
            addProperty("role", "user")
            addProperty("content", prompt)
          },
        )
      },
    )

    val response =
      restTemplate.postForEntity<OpenaiCompletionResponse>(
        openaiMachineTranslationProperties.apiEndpoint,
        HttpEntity(requestBody.toString(), headers),
        OpenaiCompletionResponse::class.java,
      )

    return response.body?.choices?.first()?.message?.content
      ?: throw RuntimeException(response.toString())
  }

  companion object {
    class OpenaiCompletionResponse {
      @JsonProperty("choices")
      var choices: List<OpenaiChoice>? = null
    }

    class OpenaiChoice {
      @JsonProperty("message")
      var message: OpenaiMessage? = null
    }

    class OpenaiMessage {
      @JsonProperty("content")
      var content: String? = null
    }
  }
}
