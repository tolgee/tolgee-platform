package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.SetTranslationsStateStateRequest
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewDemotionServiceImpl
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewHumanActionsTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var demotionService: AiReviewDemotionServiceImpl

  lateinit var testData: AiReviewTestData
  lateinit var reviewer: UserAccount

  private val projectUrl get() = "/v2/projects/${testData.project.id}"
  private val aiReviewUrl get() = "$projectUrl/translations/${testData.deTranslation.id}/ai-review"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 88) {
      rule = testData.rule
      threshold = 85
      promptName = "Default review prompt"
      providerName = "server-provider"
      model = "gpt-test"
    }
    reviewer =
      testData.root
        .addUserAccount { username = "ai_review_state_editor" }
        .self
    testData.projectBuilder.addPermission {
      user = reviewer
      type = null
      scopes =
        arrayOf(
          Scope.KEYS_VIEW,
          Scope.TRANSLATIONS_VIEW,
          Scope.TRANSLATIONS_EDIT,
          Scope.TRANSLATIONS_STATE_EDIT,
        )
    }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `confirm keeps REVIEWED, clears auto and resolves the row`() {
    save()
    performAuthPut("$aiReviewUrl/confirm", null).andIsOk

    val translation = translation(testData.deTranslation.id)
    translation.state.assert.isEqualTo(TranslationState.REVIEWED)
    translation.auto.assert.isFalse()
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.CONFIRMED)
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    row.resolvedAt.assert.isNotNull()
  }

  @Test
  fun `reject sets TRANSLATED, clears auto and resolves the row`() {
    save()
    performAuthPut("$aiReviewUrl/reject", null).andIsOk

    val translation = translation(testData.deTranslation.id)
    translation.state.assert.isEqualTo(TranslationState.TRANSLATED)
    translation.auto.assert.isFalse()
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.REJECTED)
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
  }

  @Test
  fun `confirm on a BELOW_THRESHOLD row returns 400`() {
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.BELOW_THRESHOLD, 50)
    save()
    performAuthPut(
      "$projectUrl/translations/${testData.frTranslation.id}/ai-review/confirm",
      null,
    ).andIsBadRequest
  }

  @Test
  fun `reject without state-edit permission returns 403`() {
    save()
    userAccount = testData.translator
    performAuthPut("$aiReviewUrl/reject", null).andIsForbidden
  }

  @Test
  fun `confirm on a protected branch without branch modify permission returns 403`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.BRANCHING)
    testData.project.useBranching = true
    val branch =
      testData.projectBuilder
        .addBranch {
          name = "protected"
          isProtected = true
        }.self
    val branched = testData.addKeyWithTranslations("branched.key")
    branched.keyBuilder.self.branch = branch
    branched.de.self.state = TranslationState.REVIEWED
    testData.addReview(branched.de, AiReviewStatus.APPROVED, 88)
    save()

    userAccount = reviewer
    performAuthPut(
      "$projectUrl/translations/${branched.de.self.id}/ai-review/confirm",
      null,
    ).andIsForbidden
  }

  @Test
  fun `un-review with the set-state endpoint marks REJECTED and resolves the row`() {
    save()
    performAuthPut(
      "$projectUrl/translations/${testData.deTranslation.id}/set-state/TRANSLATED",
      null,
    ).andIsOk

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.REJECTED)
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `un-review without the feature does not mark REJECTED`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthPut(
      "$projectUrl/translations/${testData.deTranslation.id}/set-state/TRANSLATED",
      null,
    ).andIsOk

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `the batch state operation marks REJECTED and keeps flags as today`() {
    save()
    val job =
      executeInNewTransaction(platformTransactionManager) {
        batchJobService.startJob(
          request =
            SetTranslationsStateStateRequest().apply {
              keyIds = listOf(testData.key.id)
              languageIds = listOf(testData.german.id)
              state = TranslationState.TRANSLATED
            },
          project = entityManager.getReference(Project::class.java, testData.project.id),
          author = entityManager.getReference(UserAccount::class.java, testData.user.id),
          type = BatchJobType.SET_TRANSLATIONS_STATE,
          isHidden = false,
        )
      }
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      batchJobService
        .getJobDtoNoCache(job.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      review(testData.deTranslation.id)?.status.assert.isEqualTo(AiReviewStatus.REJECTED)
    }
    val row = review(testData.deTranslation.id)!!
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    translation(testData.deTranslation.id).auto.assert.isTrue()
  }

  @Test
  fun `demotion by the system does not mark REJECTED`() {
    save()
    executeInNewTransaction(platformTransactionManager) {
      entityManager.find(AiReviewRule::class.java, testData.rule.id).threshold = 95
    }
    executeInNewTransaction(platformTransactionManager) {
      demotionService.demoteKeys(testData.project.id, listOf(testData.key.id))
    }

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    row.resolvedBy.assert.isNull()
  }

  @Test
  fun `GET returns the details and ruleName is null after the rule is deleted`() {
    save()
    performAuthGet(aiReviewUrl).andIsOk.andAssertThatJson {
      node("status").isEqualTo("APPROVED")
      node("score").isEqualTo(88)
      node("threshold").isEqualTo(85)
      node("ruleId").isEqualTo(testData.rule.id)
      node("ruleName").isEqualTo("UI")
      node("promptName").isEqualTo("Default review prompt")
      node("providerName").isEqualTo("server-provider")
      node("model").isEqualTo("gpt-test")
      node("dryRun").isEqualTo(false)
      node("resolvedBy").isNull()
    }

    performAuthDelete("$projectUrl/ai-review-rules/${testData.rule.id}").andIsOk
    performAuthGet(aiReviewUrl).andIsOk.andAssertThatJson {
      node("status").isEqualTo("APPROVED")
      node("ruleId").isNull()
      node("ruleName").isNull()
    }
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)?.also { it.resolvedBy?.id }
    }

  private fun translation(id: Long) =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(id)
    }
}
