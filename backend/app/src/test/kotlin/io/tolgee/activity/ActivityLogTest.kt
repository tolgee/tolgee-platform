package io.tolgee.activity

import com.posthog.server.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobService
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitFor
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.JsonAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import java.math.BigDecimal

@SpringBootTest
class ActivityLogTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BaseTestData

  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  lateinit var batchJobService: BatchJobService

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
  }

  private fun prepareTestData() {
    testData = BaseTestData()
    testData.user.name = "Franta"
    testData.projectBuilder.apply {
      addKey {
        name = "key"
      }
    }
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it stores and returns translation set activity`() {
    prepareTestData()
    performProjectAuthPut(
      "translations",
      mapOf("key" to "key", "translations" to mapOf(testData.englishLanguage.tag to "Test")),
    ).andIsOk

    performProjectAuthGet("activity").andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.activities[0]") {
        node("revisionId").isNumber
        node("timestamp").isValidTimestamp()
        node("author").isValidAuthor()
        node("type").isString.isEqualTo("SET_TRANSLATIONS")
        node("modifiedEntities") {
          node("Translation") {
            isArray.hasSize(1)
            node("[0]") {
              node("entityId").isValidId
              node("modifications").isValidTranslationModifications()
              node("relations") {
                node("key").isKeyRelation("key")
                node("language").isLanguageRelation("English", "en")
              }
            }
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns params for batch job activity`() {
    val testData = BatchJobsTestData()
    val keys = testData.addTranslationOperationData(10)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }

    val keyIds = keys.map { it.id }.toList()

    val csLanguageId =
      testData.projectBuilder
        .getLanguageByTag("cs")!!
        .self.id
    performProjectAuthPost(
      "start-batch-job/machine-translate",
      mapOf(
        "keyIds" to keyIds,
        "targetLanguageIds" to
          listOf(
            csLanguageId,
          ),
      ),
    ).andIsOk.waitForJobCompleted()

    performProjectAuthGet("activity").andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.activities[0]") {
        node("params.targetLanguageIds") {
          isArray.hasSize(1)
          node("[0]").isNumber.isEqualTo(csLanguageId.toBigDecimal())
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it publishes business event to external monitor`() {
    prepareTestData()
    performPut(
      "/v2/projects/${project.id}/translations",
      mapOf("key" to "key", "translations" to mapOf(testData.englishLanguage.tag to "Test")),
      HttpHeaders().also {
        it["Authorization"] = listOf(AuthorizedRequestFactory.getBearerTokenString(generateJwtToken(userAccount!!.id)))
        it["X-Tolgee-Utm"] = "eyJ1dG1faGVsbG8iOiJoZWxsbyJ9"
        it["X-Tolgee-SDK-Type"] = "Unreal"
        it["X-Tolgee-SDK-Version"] = "1.0.0"
      },
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "SET_TRANSLATIONS")
    params["utm_hello"].assert.isEqualTo("hello")
    params["sdkType"].assert.isEqualTo("Unreal")
    params["sdkVersion"].assert.isEqualTo("1.0.0")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `stores namespace as relation when changing translation state`() {
    val testData = BaseTestData()
    val key =
      testData.projectBuilder.addKey {
        name = "key"
      }
    key.setNamespace("ns")
    val translation =
      key
        .addTranslation {
          language = testData.englishLanguage
          text = "t"
          state = TranslationState.REVIEWED
        }.self

    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier =
      { testData.project }

    performProjectAuthPut("/translations/${translation.id}/set-state/TRANSLATED").andIsOk
    performProjectAuthGet("activity").andAssertThatJson {
      node("_embedded.activities[0].modifiedEntities.Translation[0].relations.key.relations.namespace.data.name")
        .isEqualTo("ns")
    }
  }

  private fun JsonAssert.isValidTranslationModifications() {
    node("auto") {
      node("old").isEqualTo(null)
      node("new").isEqualTo(false)
    }
    node("text") {
      node("old").isEqualTo(null)
      node("new").isEqualTo("Test")
    }
    node("state") {
      node("old").isEqualTo(null)
      node("new").isEqualTo("TRANSLATED")
    }
  }

  private fun JsonAssert.isValidAuthor() {
    node("id").isNumber
    node("username").isEqualTo("test_username")
    node("name").isEqualTo("Franta")
  }

  private fun JsonAssert.isValidTimestamp() {
    this.isNumber
      .isGreaterThan(BigDecimal(System.currentTimeMillis() - 60000))
      .isLessThan(BigDecimal(System.currentTimeMillis()))
  }

  private fun JsonAssert.isKeyRelation(keyName: String) {
    node("entityClass").isEqualTo("Key")
    node("entityId").isValidId
    node("data") {
      node("name").isEqualTo(keyName)
    }
    node("exists").isEqualTo(true)
  }

  private fun JsonAssert.isLanguageRelation(
    name: String,
    tag: String,
  ) {
    node("entityClass").isEqualTo("Language")
    node("entityId").isValidId
    node("data") {
      node("tag").isEqualTo(tag)
      node("name").isEqualTo(name)
    }
    node("exists").isEqualTo(true)
  }

  private fun ResultActions.waitForJobCompleted() =
    andAssertThatJson {
      node("id").isNumber.satisfies({
        waitFor(pollTime = 2000) {
          val job = batchJobService.findJobDto(it.toLong())
          job?.status?.completed == true
        }
      })
    }
}
