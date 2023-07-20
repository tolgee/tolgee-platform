package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.posthog.java.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.retry
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.addDays
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MvcResult
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.system.measureTimeMillis

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true"
  ]
)
class V2ExportControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @MockBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  lateinit var currentDateProvider: CurrentDateProvider

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
    clearCaches()
  }

  @AfterEach
  fun tearDown() {
    currentDateProvider.forcedDate = null
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports to json`() {
    executeInNewTransaction {
      initBaseData()
    }
    val parsed = performExport()

    assertThatJson(parsed["en.json"]!!) {
      node("Z key").isEqualTo("A translation")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it reports business event once in a day`() {
    executeInNewTransaction {
      initBaseData()
    }
    performExport()
    performExport()
    performExport()
    performExport()
    Thread.sleep(2000)
    verify(postHog, times(1)).capture(any(), eq("EXPORT"), any())
    currentDateProvider.forcedDate = currentDateProvider.date.addDays(1)
    performExport()
    performExport()
    verify(postHog, times(2)).capture(any(), eq("EXPORT"), any())
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports to single json`() {
    executeInNewTransaction {
      initBaseData()
    }
    retry {
      val response = performProjectAuthGet("export?languages=en&zip=false")
        .andDo { obj: MvcResult -> obj.asyncResult }
      response.andPrettyPrint.andAssertThatJson {
        node("Z key").isEqualTo("A translation")
      }
      assertThat(response.andReturn().response.getHeaderValue("content-type"))
        .isEqualTo("application/json")
      assertThat(response.andReturn().response.getHeaderValue("content-disposition"))
        .isEqualTo("""attachment; filename="en.json"""")
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports to single xliff`() {
    executeInNewTransaction {
      initBaseData()
    }
    retry {
      val response = performProjectAuthGet("export?languages=en&zip=false&format=XLIFF")
        .andDo { obj: MvcResult -> obj.getAsyncResult(30000) }

      assertThat(response.andReturn().response.getHeaderValue("content-type"))
        .isEqualTo("application/x-xliff+xml")
      assertThat(response.andReturn().response.getHeaderValue("content-disposition"))
        .isEqualTo("""attachment; filename="en.xlf"""")
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it filters by keyId in`() {
    testData = TranslationsTestData()
    testData.generateLotOfData(1000)
    testDataService.saveTestData(testData.root)
    prepareUserAndProject(testData)
    commitTransaction()

    val time = measureTimeMillis {
      val selectAllResult = performProjectAuthGet("translations/select-all")
        .andIsOk
        .andGetContentAsString
      val keyIds = jacksonObjectMapper().readValue<Map<String, List<Long>>>(selectAllResult)["ids"]?.take(500)
      val parsed = performExportPost(mapOf("filterKeyId" to keyIds))
      assertThatJson(parsed["en.json"]!!) {
        isObject.hasSize(499)
      }
    }

    assertThat(time).isLessThan(2000)
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `the structureDelimiter works`() {
    testData = TranslationsTestData()
    testData.generateScopedData()
    testDataService.saveTestData(testData.root)
    prepareUserAndProject(testData)
    commitTransaction()

    performExport("structureDelimiter=").let { parsed ->
      assertThatJson(parsed["en.json"]!!) {
        node("hello\\.i\\.am\\.scoped").isEqualTo("yupee!")
      }
    }
    performExport("structureDelimiter=+").let { parsed ->
      assertThatJson(parsed["en.json"]!!) {
        node("hello.i.am.plus.scoped").isEqualTo("yupee!")
      }
    }
    performExport("").let { parsed ->
      assertThatJson(parsed["en.json"]!!) {
        node("hello.i.am.scoped").isEqualTo("yupee!")
      }
    }
  }

  private fun performExport(query: String = ""): Map<String, String> {
    val mvcResult = performProjectAuthGet("export?$query")
      .andIsOk
      .andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
    return parseZip(mvcResult.response.contentAsByteArray)
  }

  private fun performExportPost(body: Any): Map<String, String> {
    val mvcResult = performProjectAuthPost("export", body)
      .andIsOk
      .andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
    return parseZip(mvcResult.response.contentAsByteArray)
  }

  private fun parseZip(responseContent: ByteArray): Map<String, String> {
    val byteArrayInputStream = ByteArrayInputStream(responseContent)
    val zipInputStream = ZipInputStream(byteArrayInputStream)

    return zipInputStream.use {
      generateSequence {
        it.nextEntry
      }.filterNot { it.isDirectory }
        .map { it.name to zipInputStream.bufferedReader().readText() }.toMap()
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports to json with namespaces`() {
    val namespacesTestData = NamespacesTestData()
    testDataService.saveTestData(namespacesTestData.root)
    projectSupplier = { namespacesTestData.projectBuilder.self }
    userAccount = namespacesTestData.user

    val parsed = performExport()

    assertThatJson(parsed["ns-1/en.json"]!!) {
      node("key").isEqualTo("hello")
    }
    assertThatJson(parsed["en.json"]!!) {
      node("key").isEqualTo("hello")
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports only allowed languages`() {
    val testData = LanguagePermissionsTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.viewEnOnlyUser

    val parsed = performExport()
    val files = parsed.keys
    files.assert.containsExactly("en.json")
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports all languages by default`() {
    val testData = TestDataBuilder()
    val user = testData.addUserAccount {
      username = "user"
    }
    val projectBuilder = testData.addProject {
      name = "Oh my project"
      organizationOwner = user.defaultOrganizationBuilder.self
    }

    val langs = arrayOf(
      projectBuilder.addEnglish(),
      projectBuilder.addCzech(),
      projectBuilder.addGerman(),
      projectBuilder.addFrench()
    )

    val key = projectBuilder.addKey { name = "key" }.self
    langs.forEach { lang ->
      projectBuilder.addTranslation {
        this.language = lang.self
        this.key = key
        this.text = "yey"
      }
    }

    testDataService.saveTestData(testData)

    projectSupplier = { projectBuilder.self }
    userAccount = user.self

    val parsed = performExport()
    val files = parsed.keys
    files.assert.containsExactlyInAnyOrder(*langs.map { "${it.self.tag}.json" }.toTypedArray())
  }

  private fun initBaseData() {
    testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    prepareUserAndProject(testData)
  }

  private fun prepareUserAndProject(testData: TranslationsTestData) {
    userAccount = testData.user
    projectSupplier = { testData.project }
  }
}
