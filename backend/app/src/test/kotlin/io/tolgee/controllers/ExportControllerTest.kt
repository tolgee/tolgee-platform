package io.tolgee.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotModified
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Language
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ExportControllerTest : ProjectAuthControllerTest() {
  private lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
    userAccount = testData.user
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun exportZipJson() {
    val mvcResult =
      performProjectAuthGet("export/jsonZip")
        .andIsOk
        .andDo { obj: MvcResult -> obj.getAsyncResult(60000) }
        .andReturn()
    mvcResult.response
    val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
    project.languages.forEach(
      Consumer { l: Language ->
        val name = l.tag + ".json"
        Assertions.assertThat(fileSizes).containsKey(name)
      },
    )
  }

  @Test
  @Transactional
  @ProjectApiKeyAuthTestMethod
  fun exportZipJsonWithApiKey() {
    val mvcResult =
      performProjectAuthGet("export/jsonZip")
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andDo { obj: MvcResult -> obj.asyncResult }
        .andReturn()
    val fileSizes = parseZip(mvcResult.response.contentAsByteArray)
    project.languages.forEach(
      Consumer { l: Language ->
        val name = l.tag + ".json"
        Assertions.assertThat(fileSizes).containsKey(name)
      },
    )
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT])
  fun exportZipJsonApiKeyPermissionFail() {
    performProjectAuthGet("export/jsonZip").andIsForbidden
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  fun `exports only permitted langs`() {
    val testData = LanguagePermissionsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.viewEnOnlyUser
    projectSupplier = { testData.project }
    val result =
      performProjectAuthGet("export/jsonZip")
        .andDo { obj: MvcResult -> obj.asyncResult }
        .andReturn()
    val fileSizes = parseZip(result.response.contentAsByteArray)
    Assertions.assertThat(fileSizes).containsOnlyKeys("en.json")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns export with last modified header`() {
    val now = Date()
    setForcedDate(now)
    val lastModified = performAndGetLastModified()
    assertEqualsDate(lastModified, now)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns 304 when export not modified`() {
    val now = Date()
    setForcedDate(now)
    val lastModified = performAndGetLastModified()
    performWithIfModifiedSince(lastModified).andIsNotModified
  }

  @AfterEach
  fun clearDate() {
    clearForcedDate()
    testDataService.cleanTestData(testData.root)
  }

  private fun performWithIfModifiedSince(lastModified: String?): ResultActions {
    val headers = HttpHeaders()
    headers["x-api-key"] = apiKeyService.create(userAccount!!, scopes = setOf(Scope.TRANSLATIONS_VIEW), project).key
    headers["If-Modified-Since"] = lastModified
    return performGet("/api/project/export/jsonZip", headers)
  }

  private fun performAndGetLastModified(): String? =
    performProjectAuthGet("export/jsonZip")
      .andIsOk.lastModified()

  private fun ResultActions.lastModified() = this.andReturn().response.getHeader("Last-Modified")

  private fun assertEqualsDate(
    lastModified: String?,
    now: Date,
  ) {
    val zdt: ZonedDateTime = ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
    (zdt.toInstant().toEpochMilli() / 1000).assert.isEqualTo(now.time / 1000)
  }

  private fun parseZip(responseContent: ByteArray): Map<String, Long> {
    val byteArrayInputStream = ByteArrayInputStream(responseContent)
    val zipInputStream = ZipInputStream(byteArrayInputStream)
    val result = HashMap<String, Long>()
    var nextEntry: ZipEntry?
    while (zipInputStream.nextEntry.also {
        nextEntry = it
      } != null
    ) {
      result[nextEntry!!.name] = nextEntry!!.size
    }
    return result
  }
}
