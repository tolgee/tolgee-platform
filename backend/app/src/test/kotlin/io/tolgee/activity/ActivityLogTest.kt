package io.tolgee.activity

import com.posthog.java.PostHog
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.AuthorizedRequestFactory
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.JsonAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import java.math.BigDecimal

class ActivityLogTest : ProjectAuthControllerTest("/v2/projects/") {

  private lateinit var testData: BaseTestData

  @MockBean
  @Autowired
  lateinit var postHog: PostHog

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
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
  fun `it publishes business event to external monitor`() {
    performPut(
      "/v2/projects/${project.id}/translations",
      mapOf("key" to "key", "translations" to mapOf(testData.englishLanguage.tag to "Test")),
      HttpHeaders().also {
        it["Authorization"] = listOf(AuthorizedRequestFactory.getBearerTokenString(generateJwtToken(userAccount!!.id)))
        it["X-Tolgee-Utm"] = "eyJ1dG1faGVsbG8iOiJoZWxsbyJ9"
        it["X-Tolgee-SDK-Type"] = "Unreal"
        it["X-Tolgee-SDK-Version"] = "1.0.0"
      }
    ).andIsOk

    var params: Map<String, Any?>? = null
    waitForNotThrowing(timeout = 10000) {
      verify(postHog, times(1)).capture(
        any(), eq("SET_TRANSLATIONS"),
        argThat {
          params = this
          true
        }
      )
    }
    params!!["utm_hello"].assert.isEqualTo("hello")
    params!!["sdkType"].assert.isEqualTo("Unreal")
    params!!["sdkVersion"].assert.isEqualTo("1.0.0")
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

  private fun JsonAssert.isLanguageRelation(name: String, tag: String) {
    node("entityClass").isEqualTo("Language")
    node("entityId").isValidId
    node("data") {
      node("tag").isEqualTo(tag)
      node("name").isEqualTo(name)
    }
    node("exists").isEqualTo(true)
  }
}
