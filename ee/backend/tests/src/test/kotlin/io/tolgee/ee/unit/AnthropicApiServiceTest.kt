package io.tolgee.ee.unit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.ee.component.llm.AnthropicApiService
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

class AnthropicApiServiceTest {
  private lateinit var service: AnthropicApiService
  private val objectMapper = jacksonObjectMapper()
  private var capturedRequestBody: String? = null

  @BeforeEach
  fun setUp() {
    service = AnthropicApiService()
    capturedRequestBody = null
  }

  @Test
  fun `includes output_config when shouldOutputJson and format is json_schema`() {
    val config = createConfig(format = "json_schema")
    val params = createParams(shouldOutputJson = true)
    val restTemplate = createCapturingRestTemplate()

    service.translate(params, config, restTemplate)

    val bodyMap = objectMapper.readValue<Map<String, Any>>(capturedRequestBody!!)

    assertThat(bodyMap).containsKey("output_config")
    @Suppress("UNCHECKED_CAST")
    val outputConfig = bodyMap["output_config"] as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    val format = outputConfig["format"] as Map<String, Any>
    assertThat(format["type"]).isEqualTo("json_schema")
    @Suppress("UNCHECKED_CAST")
    val schema = format["schema"] as Map<String, Any>
    assertThat(schema["type"]).isEqualTo("object")
    assertThat(schema).containsKey("properties")
    @Suppress("UNCHECKED_CAST")
    val properties = schema["properties"] as Map<String, Any>
    assertThat(properties).containsKey("output")
    assertThat(properties).containsKey("contextDescription")
    assertThat(schema["required"]).isEqualTo(listOf("output", "contextDescription"))
    assertThat(schema["additionalProperties"]).isEqualTo(false)
  }

  @Test
  fun `omits output_config when format is not json_schema`() {
    val config = createConfig(format = null)
    val params = createParams(shouldOutputJson = true)
    val restTemplate = createCapturingRestTemplate()

    service.translate(params, config, restTemplate)

    val bodyMap = objectMapper.readValue<Map<String, Any>>(capturedRequestBody!!)

    assertThat(bodyMap).doesNotContainKey("output_config")
  }

  @Test
  fun `omits output_config when shouldOutputJson is false`() {
    val config = createConfig(format = "json_schema")
    val params = createParams(shouldOutputJson = false)
    val restTemplate = createCapturingRestTemplate()

    service.translate(params, config, restTemplate)

    val bodyMap = objectMapper.readValue<Map<String, Any>>(capturedRequestBody!!)

    assertThat(bodyMap).doesNotContainKey("output_config")
  }

  private fun createConfig(format: String? = null): LlmProviderInterface {
    return object : LlmProviderInterface {
      override var name = "test-anthropic"
      override var type = LlmProviderType.ANTHROPIC
      override var priority: LlmProviderPriority? = LlmProviderPriority.HIGH
      override var apiKey: String? = "test-key"
      override var apiUrl: String? = "https://api.anthropic.com"
      override var model: String? = "claude-sonnet-4-5-20250929"
      override var format: String? = format
      override var deployment: String? = null
      override var reasoningEffort: String? = null
      override var maxTokens: Long = 1000
      override var tokenPriceInCreditsInput: Double? = null
      override var tokenPriceInCreditsOutput: Double? = null
      override var attempts: List<Int>? = null
    }
  }

  private fun createParams(shouldOutputJson: Boolean): LlmParams {
    return LlmParams(
      messages =
        listOf(
          LlmParams.Companion.LlmMessage(
            type = LlmParams.Companion.LlmMessageType.TEXT,
            text = "Translate 'hello' to Czech",
          ),
        ),
      shouldOutputJson = shouldOutputJson,
      priority = LlmProviderPriority.HIGH,
    )
  }

  private fun createCapturingRestTemplate(): RestTemplate {
    val responseJson =
      """
      {"content":[{"text":"result"}],"usage":{"input_tokens":10,"output_tokens":5}}
      """.trimIndent()

    val factory =
      ClientHttpRequestFactory { uri, httpMethod ->
        CapturingClientHttpRequest(uri, httpMethod, responseJson) { body ->
          capturedRequestBody = body
        }
      }

    return RestTemplate(factory)
  }

  private class CapturingClientHttpRequest(
    private val uri: URI,
    private val httpMethod: HttpMethod,
    private val responseJson: String,
    private val onBody: (String) -> Unit,
  ) : ClientHttpRequest {
    private val outputStream = ByteArrayOutputStream()
    private val headers = HttpHeaders()

    override fun getMethod() = httpMethod

    override fun getURI() = uri

    override fun getHeaders() = headers

    override fun getBody(): OutputStream = outputStream

    override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()

    override fun execute(): ClientHttpResponse {
      onBody(outputStream.toString(Charsets.UTF_8))
      return StubClientHttpResponse(responseJson)
    }
  }

  private class StubClientHttpResponse(
    private val body: String,
  ) : ClientHttpResponse {
    private val headers =
      HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
      }

    override fun getStatusCode() = HttpStatus.OK

    override fun getHeaders() = headers

    override fun getBody(): InputStream = ByteArrayInputStream(body.toByteArray())

    override fun close() {}

    @Deprecated("Deprecated in Java")
    override fun getRawStatusCode() = 200

    @Deprecated("Deprecated in Java")
    override fun getStatusText() = "OK"
  }
}
