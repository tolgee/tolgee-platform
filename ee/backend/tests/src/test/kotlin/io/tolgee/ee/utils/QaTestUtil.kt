package io.tolgee.ee.utils

import io.tolgee.ee.development.QaTestData
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckRunnerService
import io.tolgee.ee.service.qa.QaIssueService
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.ProjectQaConfigRepository
import io.tolgee.repository.qa.TranslationQaIssueRepository
import org.springframework.stereotype.Component

@Component
class QaTestUtil(
  private val qaCheckRunnerService: QaCheckRunnerService,
  private val qaIssueService: QaIssueService,
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val projectQaConfigRepository: ProjectQaConfigRepository,
) {
  lateinit var testData: QaTestData

  fun saveDefaultQaConfig() {
    projectQaConfigRepository.save(testData.createDefaultQaConfig())
  }

  /**
   * Runs all enabled QA checks on [translation] and persists the results.
   * Compares against the English base translation from test data.
   */
  fun runChecksAndPersist(
    translation: Translation,
    text: String = translation.text ?: "",
  ) {
    val params =
      QaCheckParams(
        baseText = testData.enTranslation.text,
        text = text,
        baseLanguageTag = "en",
        languageTag = "fr",
      )
    val results = qaCheckRunnerService.runEnabledChecks(testData.project.id, params)
    qaIssueService.replaceIssuesForTranslation(translation, results)
  }

  fun getPersistedIssues(translation: Translation): List<TranslationQaIssue> =
    qaIssueRepository.findAllByTranslationId(translation.id)

  fun ignoreIssue(issue: TranslationQaIssue) {
    qaIssueService.ignoreIssue(testData.project.id, issue.translation.id, issue.id)
  }
}
