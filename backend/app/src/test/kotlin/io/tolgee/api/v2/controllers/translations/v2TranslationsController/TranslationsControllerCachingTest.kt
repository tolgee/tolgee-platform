package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andIsNotModified
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.ResultActions
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
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
  fun `works when data change`() {
    val now = Date()
    setForcedDate(now)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    val lastModified = performAndGetLastModified()
    performWithIsModifiedSince(lastModified).andIsNotModified

    val newNow = Date(Date().time + 50000)
    setForcedDate(newNow)
    translationService.setTranslationText(testData.aKey, testData.englishLanguage, "This was changed!")

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

  private fun performAndGetLastModified(): String? =
    performProjectAuthGet("/translations/en,de")
      .andIsOk.lastModified()

  private fun ResultActions.lastModified() = this.andReturn().response.getHeader("Last-Modified")

  private fun assertEqualsDate(
    lastModified: String?,
    now: Date,
  ) {
    val zdt: ZonedDateTime = ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
    (zdt.toInstant().toEpochMilli() / 1000).assert.isEqualTo(now.time / 1000)
  }
}
