package io.tolgee.ee.api.v2.controllers.qa

import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.qa.QaCheckIssueIgnoreRequest
import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.utils.QaTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNoContent
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
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
  private lateinit var qaIssueRepository: TranslationQaIssueRepository

  @Autowired
  lateinit var qa: QaTestUtil

  lateinit var testData: QaTestData

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
    qa.testData = testData
    qa.saveDefaultQaConfig()
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
    qa.runChecksAndPersist(testData.frTranslation)

    performAuthGet(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues",
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
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues",
    ).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `replaces issues on re-run`() {
    qa.runChecksAndPersist(testData.frTranslation)

    // Re-run with clean text — no issues
    qa.runChecksAndPersist(testData.frTranslation, text = "Bonjour monde.")

    performAuthGet(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues",
    ).andIsOk.andAssertThatJson {
      node("_embedded").isAbsent()
    }
  }

  @Test
  fun `translation list includes qaIssueCount`() {
    qa.runChecksAndPersist(testData.frTranslation)

    performAuthGet(
      "/v2/projects/${testData.project.id}/translations?sort=id",
    ).andIsOk.andAssertThatJson {
      node("_embedded.keys[0].translations.fr.qaIssueCount").isNumber.isGreaterThan(java.math.BigDecimal.ZERO)
      node("_embedded.keys[0].translations.en.qaIssueCount").isNumber.isEqualTo(java.math.BigDecimal.ZERO)
    }
  }

  @Test
  fun `ignores a QA issue`() {
    qa.runChecksAndPersist(testData.frTranslation)
    val issue = qa.getPersistedIssues(testData.frTranslation).first()

    performAuthPut(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/${issue.id}/ignore",
      null,
    ).andIsOk

    val updatedIssue = qaIssueRepository.findById(issue.id).get()
    assertThat(updatedIssue.state.name).isEqualTo("IGNORED")
  }

  @Test
  fun `unignores a QA issue`() {
    qa.runChecksAndPersist(testData.frTranslation)
    val issue = qa.getPersistedIssues(testData.frTranslation).first()

    // First ignore it
    performAuthPut(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/${issue.id}/ignore",
      null,
    ).andIsOk

    // Then unignore it
    performAuthPut(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/${issue.id}/unignore",
      null,
    ).andIsOk

    val updatedIssue = qaIssueRepository.findById(issue.id).get()
    assertThat(updatedIssue.state.name).isEqualTo("OPEN")
  }

  @Test
  fun `ignored state is preserved when issues are re-persisted`() {
    qa.runChecksAndPersist(testData.frTranslation)
    val issueToIgnore = qa.getPersistedIssues(testData.frTranslation).first()
    qa.ignoreIssue(issueToIgnore)

    // Re-persist with the same results (simulating a re-save)
    qa.runChecksAndPersist(testData.frTranslation)

    // The matching issue should still be IGNORED
    val matchingIssue =
      qa.getPersistedIssues(testData.frTranslation).find {
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
    qa.runChecksAndPersist(testData.frTranslation)
    val issues = qa.getPersistedIssues(testData.frTranslation)
    val totalIssues = issues.size

    // Ignore all issues
    issues.forEach { qa.ignoreIssue(it) }

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
    qa.runChecksAndPersist(testData.frTranslation)
    val issue = qa.getPersistedIssues(testData.frTranslation).first()

    val request =
      QaCheckIssueIgnoreRequest(
        type = issue.type,
        message = issue.message,
        replacement = issue.replacement,
        positionStart = issue.positionStart,
        positionEnd = issue.positionEnd,
      )

    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
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

    assertThat(qa.getPersistedIssues(testData.frTranslation)).isEmpty()

    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
      request,
    ).andIsOk

    val issuesAfter = qa.getPersistedIssues(testData.frTranslation)
    assertThat(issuesAfter).hasSize(1)
    assertThat(issuesAfter.first().state.name).isEqualTo("IGNORED")
    assertThat(issuesAfter.first().type).isEqualTo(QaCheckType.CHARACTER_CASE_MISMATCH)
    assertThat(issuesAfter.first().virtual).isTrue()
  }

  @Test
  fun `unignores existing issue by params`() {
    qa.runChecksAndPersist(testData.frTranslation)
    val issue = qa.getPersistedIssues(testData.frTranslation).first()

    // First ignore it
    qa.ignoreIssue(issue)

    val request =
      QaCheckIssueIgnoreRequest(
        type = issue.type,
        message = issue.message,
        replacement = issue.replacement,
        positionStart = issue.positionStart,
        positionEnd = issue.positionEnd,
      )

    performAuthDelete(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
      request,
    ).andIsOk

    val updatedIssue = qaIssueRepository.findById(issue.id).get()
    assertThat(updatedIssue.state.name).isEqualTo("OPEN")
  }

  @Test
  fun `unignoring a virtual issue by id deletes it`() {
    val request =
      QaCheckIssueIgnoreRequest(
        type = QaCheckType.CHARACTER_CASE_MISMATCH,
        message = QaIssueMessage.QA_CASE_CAPITALIZE,
        replacement = null,
        positionStart = 0,
        positionEnd = 5,
      )

    // Create a virtual issue via suppression
    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
      request,
    ).andIsOk

    val issues = qa.getPersistedIssues(testData.frTranslation)
    assertThat(issues).hasSize(1)
    val virtualIssue = issues.first()
    assertThat(virtualIssue.virtual).isTrue()

    // Unignore by id should delete the virtual issue
    performAuthPut(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/${virtualIssue.id}/unignore",
      null,
    ).andIsOk

    assertThat(qa.getPersistedIssues(testData.frTranslation)).isEmpty()
  }

  @Test
  fun `removing a virtual issue suppression deletes it`() {
    val request =
      QaCheckIssueIgnoreRequest(
        type = QaCheckType.CHARACTER_CASE_MISMATCH,
        message = QaIssueMessage.QA_CASE_CAPITALIZE,
        replacement = null,
        positionStart = 0,
        positionEnd = 5,
      )

    // Create a virtual issue via suppression
    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
      request,
    ).andIsOk

    assertThat(qa.getPersistedIssues(testData.frTranslation).filter { it.virtual }).hasSize(1)

    // Remove suppression should delete the virtual issue
    performAuthDelete(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
      request,
    ).andIsOk

    assertThat(qa.getPersistedIssues(testData.frTranslation).filter { it.virtual }).isEmpty()
  }

  @Test
  fun `virtual flag is not inherited during re-persistence`() {
    val request =
      QaCheckIssueIgnoreRequest(
        type = QaCheckType.CHARACTER_CASE_MISMATCH,
        message = QaIssueMessage.QA_CASE_CAPITALIZE,
        replacement = "B",
        positionStart = 0,
        positionEnd = 1,
      )

    // Create a virtual ignored issue via suppression
    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
      request,
    ).andIsOk

    val virtualIssues = qa.getPersistedIssues(testData.frTranslation)
    assertThat(virtualIssues).hasSize(1)
    assertThat(virtualIssues.first().virtual).isTrue()

    // Run QA checks that produce the same issue
    qa.runChecksAndPersist(testData.frTranslation)

    // The re-persisted issue should NOT be virtual
    val caseIssue =
      qa
        .getPersistedIssues(
          testData.frTranslation,
        ).find { it.type == QaCheckType.CHARACTER_CASE_MISMATCH }
    assertThat(caseIssue).isNotNull
    assertThat(caseIssue!!.virtual).isFalse()
    // But ignored state SHOULD be inherited
    assertThat(caseIssue.state.name).isEqualTo("IGNORED")
  }

  @Test
  fun `removing non-existing suppression returns 204`() {
    val request =
      QaCheckIssueIgnoreRequest(
        type = QaCheckType.CHARACTER_CASE_MISMATCH,
        message = QaIssueMessage.QA_CASE_CAPITALIZE,
        replacement = null,
        positionStart = 0,
        positionEnd = 5,
      )

    performAuthDelete(
      "/v2/projects/${testData.project.id}/translations/${testData.frTranslation.id}/qa-issues/suppressions",
      request,
    ).andIsNoContent

    assertThat(qa.getPersistedIssues(testData.frTranslation)).isEmpty()
  }
}
