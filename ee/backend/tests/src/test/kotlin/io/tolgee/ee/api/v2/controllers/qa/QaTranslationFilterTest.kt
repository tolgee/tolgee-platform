package io.tolgee.ee.api.v2.controllers.qa

import com.posthog.server.PostHog
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.QaTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.qa.QaCheckBatchService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
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

  @Autowired
  private lateinit var qaCheckBatchService: QaCheckBatchService

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

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
  fun `filterQaCheckType matches keys with that check type in OPEN state in the requested language`() {
    performAuthGet(
      "$translationsUrl?filterQaCheckType=fr,PUNCTUATION_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("key-with-issues")
    }
  }

  @Test
  fun `filterQaCheckType does not match issues in other languages`() {
    performAuthGet(
      "$translationsUrl?filterQaCheckType=en,PUNCTUATION_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @Test
  fun `filterQaCheckType supports multiple check types in same language (OR semantics)`() {
    performAuthGet(
      "$translationsUrl?filterQaCheckType=fr,PUNCTUATION_MISMATCH&filterQaCheckType=fr,CHARACTER_CASE_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("key-with-issues")
    }
  }

  @Test
  fun `filterQaCheckType OR-combines across languages`() {
    performAuthGet(
      "$translationsUrl?languages=en&languages=fr&languages=de" +
        "&filterQaCheckType=fr,PUNCTUATION_MISMATCH&filterQaCheckType=de,SPACES_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(2)
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("key-with-issues")
      }
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("key-with-de-issues")
      }
    }
  }

  @Test
  fun `filterQaCheckType silently drops entries for languages not in the returned set`() {
    performAuthGet(
      "$translationsUrl?filterQaCheckType=de,SPACES_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(7)
    }
  }

  @Test
  fun `disabling a language drops it from filterQaCheckType while keeping enabled languages`() {
    performAuthGet(
      "$translationsUrl?languages=en&languages=de&languages=es" +
        "&filterQaCheckType=es,PUNCTUATION_MISMATCH&filterQaCheckType=de,SPACES_MISMATCH",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.hasSize(1)
      node("_embedded.keys[0].keyName").isEqualTo("key-with-de-issues")
    }
  }

  @Test
  fun `filterHasQaIssuesInLang for a disabled-only language is ignored`() {
    performAuthGet("$translationsUrl?filterHasQaIssuesInLang=es").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(7)
    }
  }

  @Test
  fun `filterQaChecksStaleInLang for a disabled-only language is ignored`() {
    performAuthGet("$translationsUrl?filterQaChecksStaleInLang=es").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(7)
    }
  }

  @Test
  fun `qaIssueCount is zero for a disabled language in the translation view`() {
    performAuthGet("$translationsUrl?languages=es&languages=de").andIsOk.andAssertThatJson {
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("key-with-issues")
        assertThatJson(it).node("translations.es.qaIssueCount").isEqualTo(0)
      }
      node("_embedded.keys").isArray.anySatisfy {
        assertThatJson(it).node("keyName").isEqualTo("key-with-de-issues")
        assertThatJson(it).node("translations.de.qaIssueCount").isEqualTo(1)
      }
    }
  }

  @Test
  fun `recheck does not delete QA issues for a disabled language`() {
    val esTranslationId = testData.disabledLangTranslationWithIssues.id
    val countBefore = qaIssueRepository.findAllByTranslationId(esTranslationId).size
    countBefore.assert.isGreaterThan(0)

    qaCheckBatchService.runChecksAndPersist(
      testData.project.id,
      testData.keyWithIssues.id,
      testData.spanishLanguage.id,
      null,
    )

    qaIssueRepository
      .findAllByTranslationId(esTranslationId)
      .size.assert
      .isEqualTo(countBefore)
  }

  @Test
  fun `filterQaCheckType returns 400 when value is malformed`() {
    performAuthGet(
      "$translationsUrl?filterQaCheckType=fr,NOT_A_REAL_TYPE",
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("filter_by_value_qa_check_type_not_valid")
    }
  }

  @Test
  fun `returns all translations when no QA filter`() {
    performAuthGet(translationsUrl).andIsOk.andAssertThatJson {
      // test-key, key-without-fr-translation, fresh-fr-key, stale-fr-key, key-with-issues,
      // ignored-only-key, key-with-de-issues
      node("_embedded.keys").isArray.hasSize(7)
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
      node("page.totalElements").isEqualTo(7)
    }
  }
}
