package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.posthog.java.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.development.testDataBuilder.data.NamespacesTestData
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.retry
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.addDays
import io.tolgee.util.addSeconds
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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.web.servlet.MvcResult
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.system.measureTimeMillis

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class V2ExportControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  var testData: TranslationsTestData? = null
  var namespacesTestData: NamespacesTestData? = null
  var languagePermissionsTestData: LanguagePermissionsTestData? = null

  @MockBean
  @Autowired
  lateinit var postHog: PostHog

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
    clearCaches()
  }

  @AfterEach
  fun tearDown() {
    clearForcedDate()
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports to json`() {
    retryingOnCommonIssues {
      executeInNewTransaction {
        initBaseData()
      }
      val parsed = performExport()

      assertThatJson(parsed["en.json"]!!) {
        node("Z key").isEqualTo("A translation")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it reports business event once in a day`() {
    retryingOnCommonIssues {
      clearCaches()
      initBaseData()
      Mockito.reset(postHog)
      try {
        currentDateProvider.forcedDate = currentDateProvider.date
        performExport()
        performExport()
        waitForNotThrowing(pollTime = 50, timeout = 3000) {
          verify(postHog, times(1)).capture(any(), eq("EXPORT"), any())
        }
        setForcedDate(currentDateProvider.date.addDays(1).addSeconds(1))
        performExport()
        waitForNotThrowing(pollTime = 50, timeout = 3000) {
          verify(postHog, times(2)).capture(any(), eq("EXPORT"), any())
        }
      } finally {
        Mockito.reset(postHog)
      }
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports to single json`() {
    retryingOnCommonIssues {
      executeInNewTransaction {
        initBaseData()
      }
      val response =
        performProjectAuthGet("export?languages=en&zip=false")
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
    retryingOnCommonIssues {
      executeInNewTransaction {
        initBaseData()
      }
      val response =
        performProjectAuthGet("export?languages=en&zip=false&format=XLIFF")
          .andDo { obj: MvcResult -> obj.getAsyncResult(30000) }

      assertThat(response.andReturn().response.getHeaderValue("content-type"))
        .isEqualTo("application/x-xliff+xml")
      assertThat(response.andReturn().response.getHeaderValue("content-disposition"))
        .isEqualTo("""attachment; filename="en.xliff"""")
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it filters by keyId in`() {
    retryingOnCommonIssues {
      testData = TranslationsTestData()
      testData!!.generateLotOfData(1000)
      testDataService.saveTestData(testData!!.root)
      prepareUserAndProject(testData!!)
      commitTransaction()

      val time =
        measureTimeMillis {
          val selectAllResult =
            performProjectAuthGet("translations/select-all")
              .andIsOk
              .andGetContentAsString
          val keyIds =
            jacksonObjectMapper()
              .readValue<Map<String, List<Long>>>(selectAllResult)["ids"]?.take(500)
          val parsed = performExportPost(mapOf("filterKeyId" to keyIds))
          assertThatJson(parsed["en.json"]!!) {
            isObject.hasSize(499)
          }
        }

      assertThat(time).isLessThan(2000)
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `the structureDelimiter works`() {
    retryingOnCommonIssues {
      testData = TranslationsTestData()
      testData!!.generateScopedData()
      testDataService.saveTestData(testData!!.root)
      prepareUserAndProject(testData!!)
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
  }

  private fun performExport(query: String = ""): Map<String, String> {
    val mvcResult =
      performProjectAuthGet("export?$query")
        .andIsOk
        .andDo { obj: MvcResult -> obj.asyncResult }.andReturn()
    return parseZip(mvcResult.response.contentAsByteArray)
  }

  private fun performExportPost(body: Any): Map<String, String> {
    val mvcResult =
      performProjectAuthPost("export", body)
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
    retryingOnCommonIssues {
      namespacesTestData = NamespacesTestData()
      testDataService.saveTestData(namespacesTestData!!.root)
      projectSupplier = { namespacesTestData!!.projectBuilder.self }
      userAccount = namespacesTestData!!.user

      val parsed = performExport()

      assertThatJson(parsed["ns-1/en.json"]!!) {
        node("key").isEqualTo("hello")
      }
      assertThatJson(parsed["en.json"]!!) {
        node("key").isEqualTo("hello")
      }
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports only allowed languages`() {
    retryingOnCommonIssues {
      languagePermissionsTestData = LanguagePermissionsTestData()
      testDataService.saveTestData(languagePermissionsTestData!!.root)
      projectSupplier = { languagePermissionsTestData!!.projectBuilder.self }
      userAccount = languagePermissionsTestData!!.viewEnOnlyUser

      val parsed = performExport()
      val files = parsed.keys
      files.assert.containsExactly("en.json")
    }
  }

  @Test
  @Transactional
  @ProjectJWTAuthTestMethod
  fun `it exports all languages by default`() {
    retryingOnCommonIssues {
      val testData = TestDataBuilder()
      val user =
        testData.addUserAccount {
          username = "user"
        }
      val projectBuilder =
        testData.addProject {
          name = "Oh my project"
          organizationOwner = user.defaultOrganizationBuilder.self
        }

      val langs =
        arrayOf(
          projectBuilder.addEnglish(),
          projectBuilder.addCzech(),
          projectBuilder.addGerman(),
          projectBuilder.addFrench(),
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

      try {
        projectSupplier = { projectBuilder.self }
        userAccount = user.self

        val parsed = performExport()
        val files = parsed.keys
        files.assert.containsExactlyInAnyOrder(*langs.map { "${it.self.tag}.json" }.toTypedArray())
      } finally {
        testDataService.cleanTestData(testData)
      }
    }
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
          namespacesTestData?.let { testDataService.cleanTestData(it.root) }
          languagePermissionsTestData?.let { testDataService.cleanTestData(it.root) }
        }
      }
    }
  }
}
