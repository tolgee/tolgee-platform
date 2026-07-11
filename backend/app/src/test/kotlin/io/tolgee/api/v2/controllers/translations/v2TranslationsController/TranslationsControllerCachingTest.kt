package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.ActivityHolder
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andIsNotModified
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.ResultActions
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class TranslationsControllerCachingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
  }

  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  @Autowired
  private lateinit var activityHolder: ActivityHolder

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  @Test
  fun `returns all with last modified`() {
    val now = Date()
    setForcedDate(now)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val lastModified = performAndGetLastModified()
    assertEqualsDate(lastModified, now)
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `returns 304 when not modified`() {
    val now = Date()
    setForcedDate(now)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val lastModified = performAndGetLastModified()
    performWithIsModifiedSince(lastModified).andIsNotModified
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `returns all with eTag`() {
    val now = Date()
    setForcedDate(now)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val eTag = performAndGetETag()
    Assertions.assertThat(eTag).isNotNull()
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `returns 304 when eTag matches`() {
    val now = Date()
    setForcedDate(now)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val eTag = performAndGetETag()
    performWithIfNoneMatch(eTag).andIsNotModified
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `works when data change with eTag`() {
    val now = Date()
    setForcedDate(now)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val eTag = performAndGetETag()
    performWithIfNoneMatch(eTag).andIsNotModified

    val newNow = Date(Date().time + 50000)
    setForcedDate(newNow)

    executeInNewTransaction {
      activityHolder.activityRevision.projectId = testData.project.id
      translationService.setTranslationText(testData.aKey, testData.englishLanguage, "This was changed!")
    }
    val newETag = performWithIfNoneMatch(eTag).andIsOk.eTag()
    Assertions.assertThat(newETag).isNotNull()
    Assertions.assertThat(newETag).isNotEqualTo(eTag)

    performWithIfNoneMatch(newETag).andIsNotModified
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `works when data change`() {
    val now = Date()
    setForcedDate(now)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val lastModified = performAndGetLastModified()
    performWithIsModifiedSince(lastModified).andIsNotModified

    val newNow = Date(Date().time + 50000)
    setForcedDate(newNow)

    executeInNewTransaction {
      activityHolder.activityRevision.projectId = testData.project.id
      translationService.setTranslationText(testData.aKey, testData.englishLanguage, "This was changed!")
    }
    val newLastModified = performWithIsModifiedSince(lastModified).andIsOk.lastModified()
    assertEqualsDate(newLastModified, newNow)

    performWithIsModifiedSince(newLastModified).andIsNotModified
  }

  fun performWithIsModifiedSince(lastModified: String?): ResultActions {
    val headers = HttpHeaders()
    headers["x-api-key"] = apiKeyService.create(userAccount!!, scopes = setOf(Scope.TRANSLATIONS_VIEW), project).key
    headers["If-Modified-Since"] = lastModified
    return performGet("/v2/projects/translations/en,de", headers)
  }

  fun performWithIfNoneMatch(eTag: String?): ResultActions {
    val headers = HttpHeaders()
    headers["x-api-key"] = apiKeyService.create(userAccount!!, scopes = setOf(Scope.TRANSLATIONS_VIEW), project).key
    headers["If-None-Match"] = eTag
    return performGet("/v2/projects/translations/en,de", headers)
  }

  private fun performAndGetLastModified(): String? =
    performProjectAuthGet("/translations/en,de")
      .andIsOk
      .lastModified()

  private fun performAndGetETag(): String? =
    performProjectAuthGet("/translations/en,de")
      .andIsOk
      .eTag()

  private fun ResultActions.lastModified() = this.andReturn().response.getHeader("Last-Modified")

  private fun ResultActions.eTag() = this.andReturn().response.getHeader("ETag")

  private fun assertEqualsDate(
    lastModified: String?,
    now: Date,
  ) {
    val zdt: ZonedDateTime = ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
    (zdt.toInstant().toEpochMilli() / 1000).assert.isEqualTo(now.time / 1000)
  }
}
