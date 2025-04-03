package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@SpringBootTest
class TolgeeTranslatorWithBatchTest {
  @Autowired
  @SpyBean
  lateinit var restTemplate: RestTemplate

  @Autowired
  lateinit var cloudTolgeeTranslateApiService: CloudTolgeeTranslateApiService

  @SpyBean
  lateinit var tokenBucketManager: TokenBucketManager

  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = currentDateProvider.date
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
  }

  @Test
  fun `it correctly handles Too Many Requests`() {
    doThrow(getMockedTooManyRequestsError()).whenever(
      restTemplate,
    ).exchange(
      argThat<String> { this.contains("/api/openai/translate") },
      argThat<HttpMethod> { this == HttpMethod.POST },
      any<HttpEntity<*>>(),
      any<ParameterizedTypeReference<Any>>(),
    )

    assertThrows<TranslationApiRateLimitException> {
      cloudTolgeeTranslateApiService.translate(
        LLMParams(
          "Helo",
          null,
          "en",
          "cs",
          null,
          null,
          true,
        ),
      )
    }.retryAt.assert.isEqualTo(currentDateProvider.date.time + 100 * 1000)

    val captor = ArgumentCaptor.forClass(Long::class.java)

    verify(tokenBucketManager, times(1))
      .setEmptyUntil(eq(CloudTolgeeTranslateApiService.Companion.BUCKET_KEY), captor.capture())

    captor.firstValue.assert.isEqualTo(currentDateProvider.date.time + 100 * 1000)

    assertThrows<TranslationApiRateLimitException> {
      cloudTolgeeTranslateApiService.translate(
        LLMParams(
          "Helo",
          null,
          "en",
          "cs",
          null,
          null,
          true,
        ),
      )
    }.retryAt.assert.isEqualTo(currentDateProvider.date.time + 100 * 1000)

    verify(tokenBucketManager, times(1))
      .setEmptyUntil(any(), any())
  }

  fun getMockedTooManyRequestsError(): HttpClientErrorException {
    val headers = HttpHeaders()
    headers.add("Retry-After", "1")
    return HttpClientErrorException.create(
      HttpStatus.TOO_MANY_REQUESTS,
      "Too Many Requests",
      HttpHeaders(),
      jacksonObjectMapper().writeValueAsBytes(
        mapOf(
          "error" to "Too Many Requests",
          "retryAfter" to 100,
        ),
      ),
      Charsets.UTF_8,
    )
  }
}
