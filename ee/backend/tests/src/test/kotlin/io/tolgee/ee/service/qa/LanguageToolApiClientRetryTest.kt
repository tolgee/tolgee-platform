package io.tolgee.ee.service.qa

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.configuration.tolgee.LanguageToolProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class LanguageToolApiClientRetryTest {
  private lateinit var client: LanguageToolApiClient
  private lateinit var mockServer: MockRestServiceServer
  private val mapper = ObjectMapper()

  @BeforeEach
  fun setup() {
    val restTemplate = RestTemplate()
    mockServer = MockRestServiceServer.createServer(restTemplate)
    val builder =
      object : RestTemplateBuilder() {
        override fun build(): RestTemplate = restTemplate

        override fun connectTimeout(connectTimeout: java.time.Duration): RestTemplateBuilder = this

        override fun readTimeout(readTimeout: java.time.Duration): RestTemplateBuilder = this
      }
    val props = TolgeeProperties(languageTool = LanguageToolProperties(url = "http://localhost:8010"))
    client = LanguageToolApiClient(props, builder)
  }

  private fun mockCheckSuccess() {
    val json = mapper.writeValueAsString(LanguageToolResponse(matches = emptyList()))
    mockServer
      .expect(requestTo("http://localhost:8010/v2/check"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))
  }

  private fun mockCheckServerError() {
    mockServer
      .expect(requestTo("http://localhost:8010/v2/check"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withServerError())
  }

  private fun mockCheckClientError() {
    mockServer
      .expect(requestTo("http://localhost:8010/v2/check"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withStatus(HttpStatus.BAD_REQUEST))
  }

  @Test
  fun `retries on transient failure and succeeds on third attempt`() {
    mockCheckServerError()
    mockCheckServerError()
    mockCheckSuccess()

    val result = client.callCheck("en-US", "Hello world.")

    assertThat(result).isEmpty()
    mockServer.verify()
  }

  @Test
  fun `gives up after three attempts and propagates the exception`() {
    mockCheckServerError()
    mockCheckServerError()
    mockCheckServerError()

    assertThatThrownBy { client.callCheck("en-US", "Hello world.") }
      .isInstanceOf(RestClientException::class.java)
    mockServer.verify()
  }

  @Test
  fun `does not retry on 4xx client errors`() {
    mockCheckClientError()

    assertThatThrownBy { client.callCheck("en-US", "Hello world.") }
      .isInstanceOf(HttpClientErrorException::class.java)
    mockServer.verify()
  }
}
