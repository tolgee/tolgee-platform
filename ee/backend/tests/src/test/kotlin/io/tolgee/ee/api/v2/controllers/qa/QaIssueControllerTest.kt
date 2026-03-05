package io.tolgee.ee.api.v2.controllers.qa

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.qa.QaCheckIssueIgnoreRequest
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNoContent
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.testing.AuthorizedControllerTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
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

  @Autowired
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

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

  @Test
  fun `ignores a QA issue`() {
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    val issueToIgnore = issues.first()

    performAuthPut(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues/${issueToIgnore.id}/ignore",
      null,
    ).andIsOk

    val updatedIssue = qaIssueRepository.findById(issueToIgnore.id).get()
    assertThat(updatedIssue.state.name).isEqualTo("IGNORED")
  }

  @Test
  fun `unignores a QA issue`() {
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    val issue = issues.first()

    // First ignore it
    performAuthPut(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues/${issue.id}/ignore",
      null,
    ).andIsOk

    // Then unignore it
    performAuthPut(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues/${issue.id}/unignore",
      null,
    ).andIsOk

    val updatedIssue = qaIssueRepository.findById(issue.id).get()
    assertThat(updatedIssue.state.name).isEqualTo("OPEN")
  }

  @Test
  fun `ignored state is preserved when issues are re-persisted`() {
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    // Ignore one issue
    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    val issueToIgnore = issues.first()
    qaIssueService.ignoreIssue(testData.project.id, issueToIgnore.id)

    // Re-persist with the same results (simulating a re-save)
    val newResults = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, newResults)

    // The matching issue should still be IGNORED
    val newIssues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    val matchingIssue =
      newIssues.find {
        it.type == issueToIgnore.type &&
          it.message == issueToIgnore.message &&
          it.positionStart == issueToIgnore.positionStart &&
          it.positionEnd == issueToIgnore.positionEnd
      }
    assertThat(matchingIssue).isNotNull
    assertThat(matchingIssue!!.state.name).isEqualTo("IGNORED")
  }

  @Test
  fun `ignored issues are not counted in qaIssueCount`() {
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    val totalIssues = issues.size

    // Ignore all issues
    issues.forEach { qaIssueService.ignoreIssue(testData.project.id, it.id) }

    performAuthGet(
      "/v2/projects/${testData.project.id}/translations?sort=id",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.fr.qaIssueCount").isNumber.isEqualTo(java.math.BigDecimal.ZERO)
    }

    // Verify there are still persisted issues (just all ignored)
    assertThat(totalIssues).isGreaterThan(0)
  }

  @Test
  fun `ignores existing issue by params`() {
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    val issue = issues.first()

    val request =
      QaCheckIssueIgnoreRequest(
        type = issue.type,
        message = issue.message,
        replacement = issue.replacement,
        positionStart = issue.positionStart,
        positionEnd = issue.positionEnd,
      )

    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues/ignore",
      request,
    ).andIsOk

    val updatedIssue = qaIssueRepository.findById(issue.id).get()
    assertThat(updatedIssue.state.name).isEqualTo("IGNORED")
  }

  @Test
  fun `ignores non-existing issue by params - creates new ignored issue`() {
    val request =
      QaCheckIssueIgnoreRequest(
        type = QaCheckType.CHARACTER_CASE_MISMATCH,
        message = QaIssueMessage.QA_CASE_CAPITALIZE,
        replacement = null,
        positionStart = 0,
        positionEnd = 5,
      )

    val issuesBefore = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    assertThat(issuesBefore).isEmpty()

    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues/ignore",
      request,
    ).andIsOk

    val issuesAfter = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    assertThat(issuesAfter).hasSize(1)
    assertThat(issuesAfter.first().state.name).isEqualTo("IGNORED")
    assertThat(issuesAfter.first().type).isEqualTo(QaCheckType.CHARACTER_CASE_MISMATCH)
  }

  @Test
  fun `unignores existing issue by params`() {
    val params =
      QaCheckParams(
        baseText = "Hello world.",
        text = "bonjour monde",
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runChecks(params)
    qaIssueService.replaceIssuesForTranslation(frTranslation, results)

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    val issue = issues.first()

    // First ignore it
    qaIssueService.ignoreIssue(testData.project.id, issue.id)

    val request =
      QaCheckIssueIgnoreRequest(
        type = issue.type,
        message = issue.message,
        replacement = issue.replacement,
        positionStart = issue.positionStart,
        positionEnd = issue.positionEnd,
      )

    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues/unignore",
      request,
    ).andIsOk

    val updatedIssue = qaIssueRepository.findById(issue.id).get()
    assertThat(updatedIssue.state.name).isEqualTo("OPEN")
  }

  @Test
  fun `unignores non-existing issue by params - returns 204`() {
    val request =
      QaCheckIssueIgnoreRequest(
        type = QaCheckType.CHARACTER_CASE_MISMATCH,
        message = QaIssueMessage.QA_CASE_CAPITALIZE,
        replacement = null,
        positionStart = 0,
        positionEnd = 5,
      )

    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${frTranslation.id}/qa-issues/unignore",
      request,
    ).andIsNoContent

    val issues = qaIssueRepository.findAllByTranslationId(frTranslation.id)
    assertThat(issues).isEmpty()
  }
}
