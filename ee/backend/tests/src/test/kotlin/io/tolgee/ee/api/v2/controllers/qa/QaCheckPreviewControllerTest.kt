package io.tolgee.ee.api.v2.controllers.qa

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.AuthorizedControllerTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class QaCheckPreviewControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var qaIssueService: QaIssueService

  @Autowired
  private lateinit var qaCheckRunnerService: QaCheckRunnerService

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

  lateinit var testData: BaseTestData
  lateinit var testKey: Key
  var frTranslation: Translation? = null

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = BaseTestData()
    testData.projectBuilder.build {
      addFrench()
      testKey =
        addKey {
          name = "test-key"
        }.build {
          addTranslation("en", "Hello world.")
          frTranslation =
            addTranslation("fr", "bonjour monde").self
        }.self
    }
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
  fun `returns empty translation issue for blank text`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "",
        "languageTag" to "en",
        "keyId" to 0,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.qaCheckResults").isArray.hasSize(1)
      node("_embedded.qaCheckResults[0].type").isEqualTo("EMPTY_TRANSLATION")
      node("_embedded.qaCheckResults[0].message").isEqualTo("qa_empty_translation")
      node("_embedded.qaCheckResults[0].replacement").isNull()
      node("_embedded.qaCheckResults[0].positionStart").isEqualTo(0)
      node("_embedded.qaCheckResults[0].positionEnd").isEqualTo(0)
    }
  }

  @Test
  fun `returns no issues for non-empty text`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "Hello world",
        "languageTag" to "en",
        "keyId" to 0,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `returns bad request when feature disabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "",
        "languageTag" to "en",
        "keyId" to 0,
      ),
    ).andIsBadRequest
  }

  @Test
  fun `returns comparison issues when base translation exists`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "bonjour monde",
        "languageTag" to "fr",
        "keyId" to testKey.id,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.qaCheckResults").isArray.isNotEmpty
      node("_embedded.qaCheckResults").isArray.anySatisfy {
        assertThatJson(it).node("type").isEqualTo("CHARACTER_CASE_MISMATCH")
        assertThatJson(it).node("message").isEqualTo("qa_case_capitalize")
      }
      node("_embedded.qaCheckResults").isArray.anySatisfy {
        assertThatJson(it).node("type").isEqualTo("PUNCTUATION_MISMATCH")
        assertThatJson(it).node("message").isEqualTo("qa_punctuation_add")
      }
    }
  }

  @Test
  fun `returns no comparison issues when keyId is zero`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "bonjour monde",
        "languageTag" to "fr",
        "keyId" to 0,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `returns params in result for punctuation check`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "Bonjour monde",
        "languageTag" to "fr",
        "keyId" to testKey.id,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.qaCheckResults").isArray.anySatisfy {
        assertThatJson(it).node("type").isEqualTo("PUNCTUATION_MISMATCH")
        assertThatJson(it).node("params.punctuation").isEqualTo(".")
      }
    }
  }

  @Test
  fun `returns no missing numbers when neither text has numbers`() {
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "Bonjour monde.",
        "languageTag" to "fr",
        "keyId" to testKey.id,
      ),
    ).andIsOk.andAssertThatJson {
      // Both "Hello world." and "Bonjour monde." have no numbers, matching case, matching
      // punctuation — no issues at all
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `preview includes ignored status from persisted issues`() {
    val translation = frTranslation!!

    // Persist QA issues for the French translation
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(translation, results)

    // Ignore one issue
    val issues = qaIssueRepository.findAllByTranslationId(translation.id)
    val issueToIgnore = issues.first()
    qaIssueService.ignoreIssue(testData.project.id, issueToIgnore.id)

    // Preview the same text — should include ignored status
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "bonjour monde",
        "languageTag" to "fr",
        "keyId" to testKey.id,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.qaCheckResults").isArray.isNotEmpty
      node("_embedded.qaCheckResults").isArray.anySatisfy {
        assertThatJson(it).node("ignored").isEqualTo(true)
        assertThatJson(it).node("persistedIssueId").isNotNull
      }
      node("_embedded.qaCheckResults").isArray.anySatisfy {
        assertThatJson(it).node("ignored").isEqualTo(false)
        assertThatJson(it).node("persistedIssueId").isNotNull
      }
    }
  }

  @Test
  fun `preview without persisted issues has default ignored values`() {
    // Preview for a language with no persisted translation
    performAuthPost(
      "/v2/projects/${testData.project.id}/qa-check/preview",
      mapOf(
        "text" to "",
        "languageTag" to "en",
        "keyId" to 0,
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.qaCheckResults").isArray.hasSize(1)
      node("_embedded.qaCheckResults[0].ignored").isEqualTo(false)
      node("_embedded.qaCheckResults[0].persistedIssueId").isNull()
    }
  }
}
