package io.tolgee.ee.api.v2.controllers.qa

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
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
class QaIssueControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var qaIssueService: QaIssueService

  @Autowired
  private lateinit var qaCheckRunnerService: QaCheckRunnerService

  lateinit var testData: BaseTestData
  lateinit var testKey: Key
  lateinit var frTranslation: Translation

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
  fun `returns persisted QA issues for a translation`() {
    // Directly run checks and persist them
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    performAuthGet(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues",
    ).andIsOk.andAssertThatJson {
      node("_embedded.qaIssues").isArray.isNotEmpty
      node("_embedded.qaIssues").isArray.anySatisfy {
        assertThatJson(it).node("type").isEqualTo("CHARACTER_CASE_MISMATCH")
        assertThatJson(it).node("state").isEqualTo("OPEN")
        assertThatJson(it).node("id").isNotNull
      }
      node("_embedded.qaIssues").isArray.anySatisfy {
        assertThatJson(it).node("type").isEqualTo("PUNCTUATION_MISMATCH")
        assertThatJson(it).node("params.punctuation").isEqualTo(".")
      }
    }
  }

  @Test
  fun `returns empty when no QA issues exist`() {
    performAuthGet(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues",
    ).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `replaces issues on re-run`() {
    // First run with issues
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    // Second run with no issues
    val cleanParams =
      QaCheckParams(
        baseText = "Hello world.",
        text = "Bonjour monde.",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val cleanResults = qaCheckRunnerService.runChecks(cleanParams)
    qaIssueService.replaceIssuesForTranslation(frTranslation, cleanResults)

    performAuthGet(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues",
    ).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `translation list includes qaIssueCount`() {
    // Create QA issues for the French translation
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    performAuthGet(
      "/v2/projects/${testData.project.id}/translations?sort=id",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.fr.qaIssueCount").isNumber.isGreaterThan(java.math.BigDecimal.ZERO)
      node("_embedded.keys[0].translations.en.qaIssueCount").isNumber.isEqualTo(java.math.BigDecimal.ZERO)
    }
  }
}
