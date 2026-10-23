package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.project.ProjectHardDeletingService
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AiReviewPersistenceTest : AbstractSpringTest() {
  @Autowired
  lateinit var aiReviewRuleRepository: AiReviewRuleRepository

  @Autowired
  lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  lateinit var projectHardDeletingService: ProjectHardDeletingService

  lateinit var testData: AiReviewTestData

  @BeforeEach
  fun setup() {
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `review row is unique per translation`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.BELOW_THRESHOLD, 50)

    assertThatThrownBy {
      testDataService.saveTestData(testData.root)
    }.rootCause().hasMessageContaining("translation_ai_review_translation")
  }

  @Test
  fun `deleting a language removes its overrides and review rows`() {
    val rule = testData.addRule { languages = mutableSetOf(testData.german, testData.french) }
    rule.addOverride(testData.german, threshold = 95)
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    testDataService.saveTestData(testData.root)

    executeInNewTransaction(platformTransactionManager) {
      languageService.hardDeleteLanguage(testData.german.id)
    }

    getReview(testData.deTranslation.id).assert.isNull()
    executeInNewTransaction(platformTransactionManager) {
      val reloaded = aiReviewRuleRepository.findById(rule.self.id).get()
      reloaded.overrides.assert.isEmpty()
      reloaded.languages
        .map { it.tag }
        .assert
        .containsExactly("fr")
    }
  }

  @Test
  fun `deleting a rule sets the review rule to null`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90) {
      rule = testData.rule
    }
    testDataService.saveTestData(testData.root)

    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleRepository.deleteById(testData.rule.id)
    }

    executeInNewTransaction(platformTransactionManager) {
      val review = translationAiReviewRepository.findByTranslationId(testData.deTranslation.id)!!
      review.rule.assert.isNull()
    }
  }

  @Test
  fun `deleting a translation deletes its review row`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    testDataService.saveTestData(testData.root)

    executeInNewTransaction(platformTransactionManager) {
      translationService.deleteByIdIn(listOf(testData.deTranslation.id))
    }

    getReview(testData.deTranslation.id).assert.isNull()
  }

  @Test
  fun `hard-deleting the project deletes rules and review rows`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90) {
      rule = testData.rule
    }
    testDataService.saveTestData(testData.root)

    executeInNewTransaction(platformTransactionManager) {
      projectHardDeletingService.hardDeleteProject(projectService.get(testData.project.id))
    }

    aiReviewRuleRepository
      .findById(testData.rule.id)
      .isPresent.assert
      .isFalse()
    getReview(testData.deTranslation.id).assert.isNull()
  }

  private fun getReview(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }
}
