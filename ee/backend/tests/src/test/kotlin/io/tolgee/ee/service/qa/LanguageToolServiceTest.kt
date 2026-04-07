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
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

class LanguageToolServiceTest {
  private lateinit var service: LanguageToolService
  private lateinit var mockServer: MockRestServiceServer
  private lateinit var tolgeeProperties: TolgeeProperties
  private val objectMapper = ObjectMapper()

  @BeforeEach
  fun setup() {
    tolgeeProperties = TolgeeProperties(languageTool = LanguageToolProperties(url = "http://localhost:8010"))

    // Create a RestTemplate and bind mock server to it
    val restTemplate = RestTemplate()
    mockServer = MockRestServiceServer.createServer(restTemplate)

    // Create a RestTemplateBuilder that returns our mocked RestTemplate
    val builder =
      object : RestTemplateBuilder() {
        override fun build(): RestTemplate = restTemplate

        override fun connectTimeout(connectTimeout: java.time.Duration): RestTemplateBuilder = this

        override fun readTimeout(readTimeout: java.time.Duration): RestTemplateBuilder = this
      }

    service = LanguageToolService(tolgeeProperties, builder)
  }

  private fun mockLanguagesResponse(vararg languages: LanguageToolLanguageInfo) {
    val json = objectMapper.writeValueAsString(languages)
    mockServer
      .expect(requestTo("http://localhost:8010/v2/languages"))
      .andExpect(method(HttpMethod.GET))
      .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))
  }

  private fun mockCheckResponse(vararg matches: LanguageToolMatch) {
    val response = LanguageToolResponse(matches = matches.toList())
    val json = objectMapper.writeValueAsString(response)
    mockServer
      .expect(requestTo("http://localhost:8010/v2/check"))
      .andExpect(method(HttpMethod.POST))
      .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))
  }

  @Test
  fun `returns empty for blank text`() {
    val results = service.check("   ", "en")
    assertThat(results).isEmpty()
  }

  @Test
  fun `throws when URL is not configured`() {
    tolgeeProperties.languageTool.url = ""
    assertThatThrownBy { service.check("Hello", "en") }
      .isInstanceOf(LanguageToolNotConfiguredException::class.java)
  }

  @Test
  fun `returns matches from API`() {
    mockLanguagesResponse(
      LanguageToolLanguageInfo(name = "English (US)", code = "en", longCode = "en-US"),
    )
    mockCheckResponse(
      LanguageToolMatch(
        message = "Possible spelling mistake",
        offset = 0,
        length = 4,
        replacements = listOf(LanguageToolReplacement("Hello")),
        rule = LanguageToolRule(id = "MORFOLOGIK_RULE_EN_US", category = LanguageToolCategory(id = "TYPOS")),
      ),
    )

    val results = service.check("Helo world", "en")
    assertThat(results).hasSize(1)
    assertThat(results[0].offset).isEqualTo(0)
    assertThat(results[0].length).isEqualTo(4)
    assertThat(results[0].replacements).hasSize(1)
    assertThat(results[0].replacements[0].value).isEqualTo("Hello")
    mockServer.verify()
  }

  @Test
  fun `resolves underscore-separated tag`() {
    // pt_BR should be normalized to pt-BR and resolved against supported languages
    mockLanguagesResponse(
      LanguageToolLanguageInfo(name = "Portuguese (Brazil)", code = "pt", longCode = "pt-BR"),
      LanguageToolLanguageInfo(name = "Portuguese (Portugal)", code = "pt", longCode = "pt-PT"),
    )

    val tag = service.resolveLanguageTag("pt_BR")
    assertThat(tag).isEqualTo("pt-BR")
  }

  @Test
  fun `falls back to base code for unknown variant`() {
    mockLanguagesResponse(
      LanguageToolLanguageInfo(name = "English (US)", code = "en", longCode = "en-US"),
      LanguageToolLanguageInfo(name = "English (GB)", code = "en", longCode = "en-GB"),
    )

    // en-XX doesn't exist, should fall back to base "en" -> "en-US" (first variant)
    val tag = service.resolveLanguageTag("en-XX")
    assertThat(tag).isEqualTo("en-US")
  }

  @Test
  fun `preserves exact language tag when supported`() {
    mockLanguagesResponse(
      LanguageToolLanguageInfo(name = "English (US)", code = "en", longCode = "en-US"),
      LanguageToolLanguageInfo(name = "English (GB)", code = "en", longCode = "en-GB"),
    )

    assertThat(service.resolveLanguageTag("en-GB")).isEqualTo("en-GB")
  }

  @Test
  fun `returns null for unsupported language`() {
    mockLanguagesResponse(
      LanguageToolLanguageInfo(name = "English (US)", code = "en", longCode = "en-US"),
    )

    assertThat(service.resolveLanguageTag("xx")).isNull()
    assertThat(service.resolveLanguageTag("xx-YY")).isNull()
  }

  @Test
  fun `resolves base language to default variant`() {
    mockLanguagesResponse(
      LanguageToolLanguageInfo(name = "English (US)", code = "en", longCode = "en-US"),
      LanguageToolLanguageInfo(name = "English (GB)", code = "en", longCode = "en-GB"),
    )

    // "en" should resolve to "en-US" (the first variant for base code "en")
    assertThat(service.resolveLanguageTag("en")).isEqualTo("en-US")
  }

  @Test
  fun `returns empty for unsupported language in check`() {
    mockLanguagesResponse(
      LanguageToolLanguageInfo(name = "English (US)", code = "en", longCode = "en-US"),
    )

    val results = service.check("Some text", "xx-unknown")
    assertThat(results).isEmpty()
  }
}
