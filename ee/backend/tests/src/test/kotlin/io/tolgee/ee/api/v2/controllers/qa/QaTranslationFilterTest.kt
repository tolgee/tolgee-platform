package io.tolgee.ee.api.v2.controllers.qa

import com.posthog.server.PostHog
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class QaTranslationFilterTest : AuthorizedControllerTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: QaTestData

  private val translationsUrl
    get() = "/v2/projects/${testData.project.id}/translations"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `filterHasQaIssuesInLang matches keys with OPEN issues, excludes keys with only IGNORED issues`() {
    performAuthGet(
      "$translationsUrl?filterHasQaIssuesInLang=fr",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("key-with-issues")
    }
  }

  @Test
  fun `filterQaCheckType matches keys with that check type in OPEN state`() {
    performAuthGet(
      "$translationsUrl?filterQaCheckType=PUNCTUATION_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("key-with-issues")
    }
  }

  @Test
  fun `filterQaCheckType supports multiple check types (OR semantics)`() {
    performAuthGet(
      "$translationsUrl?filterQaCheckType=PUNCTUATION_MISMATCH&filterQaCheckType=CHARACTER_CASE_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("key-with-issues")
    }
  }

  @Test
  fun `returns all translations when no QA filter`() {
    performAuthGet(translationsUrl).andIsOk.andAssertThatJson {
      // test-key, key-without-fr-translation, fresh-fr-key, stale-fr-key, key-with-issues, ignored-only-key
      node("_embedded.keys").isArray.hasSize(6)
    }
  }

  @Test
  fun `combines QA filter with language filter`() {
    performAuthGet(
      "$translationsUrl?filterHasQaIssuesInLang=fr&languages=fr",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("key-with-issues")
    }
  }

  @Test
  fun `filterQaChecksStaleInLang matches only translations with qaChecksStale=true in lang`() {
    // FR matches: test-key + stale-fr-key. Fresh, key-with-issues (fr fresh) and
    // ignored-only must be absent.
    performAuthGet("$translationsUrl?filterQaChecksStaleInLang=fr").andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(2)
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("test-key")
      }
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("stale-fr-key")
      }
    }

    // EN matches: test-key + key-without-fr-translation + key-with-issues (en is default
    // stale because the seeded issues are on fr). fresh / stale (en is fresh) /
    // ignored-only (en is fresh) are excluded.
    performAuthGet("$translationsUrl?filterQaChecksStaleInLang=en").andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(3)
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("test-key")
      }
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("key-without-fr-translation")
      }
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("key-with-issues")
      }
    }
  }

  @Test
  fun `stale filter is silently ignored when QA feature is disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()

    performAuthGet("$translationsUrl?filterQaChecksStaleInLang=fr").andIsOk.andAssertThatJson {
      // Filter is gated by qaEnabled — when QA is off, the param is ignored and all
      // keys are returned.
      node("page.totalElements").isEqualTo(6)
    }
  }
}
