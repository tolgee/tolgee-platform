package io.tolgee.api.v2.controllers.v2ExportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.retry
import io.tolgee.model.enums.Scope
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class V2ExportControllerCachingTest : ProjectAuthControllerTest("/v2/projects/") {
  var testData: TranslationsTestData? = null

  @BeforeEach
  fun setup() {
    clearCaches()
  }

  @AfterEach
  fun tearDown() {
    clearForcedDate()
  }

  private fun initBaseData() {
    testData = TranslationsTestData()
    testDataService.saveTestData(testData!!.root)
    prepareUserAndProject(testData!!)
  }

  private fun prepareUserAndProject(testData: TranslationsTestData) {
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `returns 304 for GET export when data not modified`() {
    retryingOnCommonIssues {
      initBaseData()

      // First request - should return data
      val firstResponse =
        performProjectAuthGet("export?languages=en&zip=false")
          .andIsOk
          .andReturn()

      val lastModifiedHeader = firstResponse.response.getHeaderValue("Last-Modified") as String
      Assertions.assertThat(lastModifiedHeader).isNotNull()

      // Second request with If-Modified-Since header - should return 304
      val headers = org.springframework.http.HttpHeaders()
      headers["If-Modified-Since"] = lastModifiedHeader
      headers["x-api-key"] = apiKeyService.create(userAccount!!, scopes = setOf(Scope.TRANSLATIONS_VIEW), project).key
      val secondResponse = performGet("/v2/projects/${project.id}/export?languages=en&zip=false", headers).andReturn()

      Assertions.assertThat(secondResponse.response.status).isEqualTo(304)
      Assertions.assertThat(secondResponse.response.contentAsByteArray).isEmpty()
      Assertions.assertThat(secondResponse.response.contentAsString).isEmpty()
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `returns 304 for GET export when eTag matches`() {
    retryingOnCommonIssues {
      initBaseData()

      // First request - should return data
      val firstResponse =
        performProjectAuthGet("export?languages=en&zip=false")
          .andIsOk
          .andReturn()

      val eTagHeader = firstResponse.response.getHeaderValue("ETag") as String
      Assertions.assertThat(eTagHeader).isNotNull()

      // Second request with If-None-Match header - should return 304
      val headers = org.springframework.http.HttpHeaders()
      headers["If-None-Match"] = eTagHeader
      headers["x-api-key"] = apiKeyService.create(userAccount!!, scopes = setOf(Scope.TRANSLATIONS_VIEW), project).key
      val secondResponse = performGet("/v2/projects/${project.id}/export?languages=en&zip=false", headers).andReturn()

      Assertions.assertThat(secondResponse.response.status).isEqualTo(304)
      Assertions.assertThat(secondResponse.response.contentAsByteArray).isEmpty()
      Assertions.assertThat(secondResponse.response.contentAsString).isEmpty()
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `returns 304 for POST export when eTag matches`() {
    retryingOnCommonIssues {
      initBaseData()

      // First request - should return data
      val firstResponse =
        performProjectAuthPost("export", mapOf("languages" to setOf("en"), "zip" to false))
          .andIsOk
          .andReturn()

      val eTagHeader = firstResponse.response.getHeaderValue("ETag") as String
      Assertions.assertThat(eTagHeader).isNotNull()

      // Second request with If-None-Match header - should return 304
      val headers = org.springframework.http.HttpHeaders()
      headers["If-None-Match"] = eTagHeader
      headers["x-api-key"] = apiKeyService.create(userAccount!!, scopes = setOf(Scope.TRANSLATIONS_VIEW), project).key
      val secondResponse =
        performPost(
          "/v2/projects/${project.id}/export",
          mapOf(
            "languages" to setOf("en"),
            "zip" to false,
          ),
          headers,
        ).andReturn()

      // With custom implementation, POST requests now return 304 (Not Modified) instead of 412
      // when conditional headers indicate the data hasn't changed, since we're using POST only
      // because we cannot provide all the params in the query - no actual modification occurs.
      Assertions.assertThat(secondResponse.response.status).isEqualTo(304)
      Assertions.assertThat(secondResponse.response.contentAsByteArray).isEmpty()
      Assertions.assertThat(secondResponse.response.contentAsString).isEmpty()
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `returns 304 for POST export when data not modified`() {
    retryingOnCommonIssues {
      initBaseData()

      // First request - should return data
      val firstResponse =
        performProjectAuthPost("export", mapOf("languages" to setOf("en"), "zip" to false))
          .andIsOk
          .andReturn()

      val lastModifiedHeader = firstResponse.response.getHeaderValue("Last-Modified") as String
      Assertions.assertThat(lastModifiedHeader).isNotNull()

      // Second request with If-Modified-Since header - should return 304
      val headers = org.springframework.http.HttpHeaders()
      headers["If-Modified-Since"] = lastModifiedHeader
      headers["x-api-key"] = apiKeyService.create(userAccount!!, scopes = setOf(Scope.TRANSLATIONS_VIEW), project).key
      val secondResponse =
        performPost(
          "/v2/projects/${project.id}/export",
          mapOf(
            "languages" to setOf("en"),
            "zip" to false,
          ),
          headers,
        ).andReturn()

      // With custom implementation, POST requests now return 304 (Not Modified) instead of 412
      // when conditional headers indicate the data hasn't changed, since we're using POST only
      // because we cannot provide all the params in the query - no actual modification occurs.
      Assertions.assertThat(secondResponse.response.status).isEqualTo(304)
      Assertions.assertThat(secondResponse.response.contentAsByteArray).isEmpty()
      Assertions.assertThat(secondResponse.response.contentAsString).isEmpty()
    }
  }

  private fun retryingOnCommonIssues(fn: () -> Unit) {
    retry(
      retries = 10,
      exceptionMatcher = matcher@{
        if (it is ConcurrentModificationException ||
          it is DataIntegrityViolationException ||
          it is NullPointerException
        ) {
          return@matcher true
        }

        if (it is IllegalStateException && it.message?.contains("End size") == true) {
          return@matcher true
        }

        false
      },
    ) {
      try {
        fn()
      } finally {
        executeInNewTransaction {
          testData?.let { testDataService.cleanTestData(it.root) }
        }
      }
    }
  }
}
