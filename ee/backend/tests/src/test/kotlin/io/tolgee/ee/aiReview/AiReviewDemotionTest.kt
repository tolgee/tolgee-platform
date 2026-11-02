package io.tolgee.ee.aiReview

import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewDemotionServiceImpl
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.SystemActor
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
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
class AiReviewDemotionTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var demotionService: AiReviewDemotionServiceImpl

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"
  private val ruleUrl get() = "$projectUrl/ai-review-rules/${testData.rule.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
    approve(testData.deTranslationBuilder)
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
  fun `a threshold raised above the stored score demotes`() {
    save()
    updateRule(threshold = 90)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val row = review(testData.deTranslation.id)!!
      row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
      row.threshold.assert.isEqualTo(90)
    }
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a lowered threshold never promotes`() {
    row(testData.frTranslationBuilder, AiReviewStatus.BELOW_THRESHOLD, 82)
    save()
    updateRule(threshold = 80)

    waitForDemotionJobs()
    review(testData.frTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    translation(testData.frTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a never override for the language demotes and deletes the row`() {
    save()
    updateRule(threshold = 85, overrideNeverLanguageId = testData.german.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id).assert.isNull()
    }
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a narrowed scope with no other rule demotes`() {
    save()
    updateRule(threshold = 85, tagIn = listOf("other"))

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id).assert.isNull()
    }
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a narrowed scope with another Active rule keeps the approval and updates the rule`() {
    val other =
      testData
        .addRule {
          name = "Fallback"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 80
          tagIn = mutableListOf("ui")
        }.self
    save()
    updateRule(threshold = 85, tagIn = listOf("other"))

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val row = review(testData.deTranslation.id)!!
      row.rule!!
        .id.assert
        .isEqualTo(other.id)
      row.threshold.assert.isEqualTo(80)
    }
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `a Dry run winner keeps the approval`() {
    save()
    updateRule(threshold = 95, mode = AiReviewRuleMode.DRY_RUN)

    waitForDemotionJobs()
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `a CONFIRMED row is never demoted`() {
    testData.frTranslation.state = TranslationState.REVIEWED
    testData.frTranslation.auto = true
    row(testData.frTranslationBuilder, AiReviewStatus.CONFIRMED, 60)
    save()
    setRuleThresholdDirectly(90)
    demoteDirectly(testData.key.id)

    review(testData.frTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.CONFIRMED)
    translation(testData.frTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `adding a tag with a Never rule demotes the key`() {
    testData.addRule {
      name = "Legal never"
      never = true
      tagIn = mutableListOf("legal")
    }
    save()

    performAuthPut("$projectUrl/keys/${testData.key.id}/tags", mapOf("name" to "legal")).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id).assert.isNull()
    }
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `moving a key to a namespace with a Never rule demotes the key`() {
    testData.project.useNamespaces = true
    testData.addRule {
      name = "Legal namespace never"
      never = true
      namespaceIn = mutableListOf("legal-ns")
    }
    save()

    performAuthPut(
      "$projectUrl/keys/${testData.key.id}/complex-update",
      mapOf("name" to testData.key.name, "namespace" to "legal-ns"),
    ).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id).assert.isNull()
    }
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `disabling a rule does not demote`() {
    save()
    performAuthPut("$ruleUrl/enabled", mapOf("enabled" to false)).andIsOk

    demotionJobCount().assert.isZero()
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `an update that disables the rule does not demote`() {
    save()
    updateRule(threshold = 99, enabled = false)

    demotionJobCount().assert.isZero()
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `creating a stricter rule demotes existing approvals`() {
    save()
    performAuthPost(
      "$projectUrl/ai-review-rules",
      mapOf(
        "name" to "Stricter",
        "mode" to AiReviewRuleMode.ACTIVE.name,
        "threshold" to 95,
        "tagIn" to listOf("ui"),
        "enabled" to true,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    }
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `re-enabling a stricter rule demotes existing approvals`() {
    val stricter =
      testData
        .addRule {
          name = "Stricter"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 95
          tagIn = mutableListOf("ui")
          enabled = false
        }.self
    save()

    performAuthPut("$projectUrl/ai-review-rules/${stricter.id}/enabled", mapOf("enabled" to true)).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    }
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `deleting a rule keeps result rows without the rule and deletes transient ones`() {
    val confirmed = testData.addKeyWithTranslations("k1")
    val rejected = testData.addKeyWithTranslations("k2")
    val belowThreshold = testData.addKeyWithTranslations("k3")
    val dryRun = testData.addKeyWithTranslations("k4")
    val failed = testData.addKeyWithTranslations("k5")
    val skipped = testData.addKeyWithTranslations("k6")
    val pending = testData.addKeyWithTranslations("k7")
    row(confirmed.de, AiReviewStatus.CONFIRMED, 95)
    row(rejected.de, AiReviewStatus.REJECTED, 40)
    row(belowThreshold.de, AiReviewStatus.BELOW_THRESHOLD, 50)
    row(dryRun.de, AiReviewStatus.DRY_RUN_PASS, 90)
    row(failed.de, AiReviewStatus.FAILED, null)
    row(skipped.de, AiReviewStatus.SKIPPED_QA_ISSUES, null)
    row(pending.de, AiReviewStatus.PENDING, null)
    save()

    performAuthDelete(ruleUrl).andIsOk

    review(testData.deTranslation.id)!!.also {
      it.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      it.rule.assert.isNull()
    }
    review(confirmed.de.self.id)!!.rule.assert.isNull()
    review(rejected.de.self.id)!!.rule.assert.isNull()
    review(belowThreshold.de.self.id)!!.rule.assert.isNull()
    review(dryRun.de.self.id).assert.isNull()
    review(failed.de.self.id).assert.isNull()
    review(skipped.de.self.id).assert.isNull()
    review(pending.de.self.id)!!.status.assert.isEqualTo(AiReviewStatus.PENDING)
  }

  @Test
  fun `demotion is logged with the system actor AI_REVIEWER`() {
    save()
    updateRule(threshold = 90)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    }
    executeInNewTransaction(platformTransactionManager) {
      val revisions =
        entityManager
          .createQuery(
            "from ActivityRevision ar where ar.projectId = :projectId and ar.systemActor = :actor",
            io.tolgee.model.activity.ActivityRevision::class.java,
          ).setParameter("projectId", testData.project.id)
          .setParameter("actor", SystemActor.AI_REVIEWER)
          .resultList
      revisions
        .flatMap { it.modifiedEntities }
        .any { it.entityClass == "Translation" && it.entityId == testData.deTranslation.id }
        .assert
        .isTrue()
    }
  }

  @Test
  fun `under Protect reviewed a raised threshold keeps REVIEWED and marks the row`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    save()
    setRuleThresholdDirectly(90)
    demoteDirectly(testData.key.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    row.threshold.assert.isEqualTo(90)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `under Protect reviewed no rule changes nothing`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    save()
    executeInNewTransaction(platformTransactionManager) {
      val rule = entityManager.find(AiReviewRule::class.java, testData.rule.id)
      rule.tagIn = mutableListOf("other")
    }
    demoteDirectly(testData.key.id)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  private fun approve(builder: TranslationBuilder) {
    builder.self.state = TranslationState.REVIEWED
    builder.self.auto = true
    testData.addReview(builder, AiReviewStatus.APPROVED, 88) {
      rule = testData.rule
      threshold = 85
    }
  }

  private fun row(
    builder: TranslationBuilder,
    status: AiReviewStatus,
    score: Int?,
  ) {
    testData.addReview(builder, status, score) {
      rule = testData.rule
    }
  }

  private fun updateRule(
    threshold: Int,
    tagIn: List<String> = listOf("ui"),
    mode: AiReviewRuleMode = AiReviewRuleMode.ACTIVE,
    overrideNeverLanguageId: Long? = null,
    enabled: Boolean = true,
  ) {
    val overrides =
      overrideNeverLanguageId?.let {
        listOf(mapOf("languageId" to it, "never" to true))
      } ?: emptyList()
    performAuthPut(
      ruleUrl,
      mapOf(
        "name" to "UI",
        "mode" to mode.name,
        "threshold" to threshold,
        "tagIn" to tagIn,
        "overrides" to overrides,
        "enabled" to enabled,
      ),
    ).andIsOk
  }

  private fun setRuleThresholdDirectly(threshold: Int) {
    executeInNewTransaction(platformTransactionManager) {
      entityManager.find(AiReviewRule::class.java, testData.rule.id).threshold = threshold
    }
  }

  private fun demoteDirectly(keyId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      demotionService.demoteKeys(testData.project.id, listOf(keyId))
    }
  }

  private fun waitForDemotionJobs() {
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      executeInNewTransaction(platformTransactionManager) {
        @Suppress("UNCHECKED_CAST")
        val statuses =
          entityManager
            .createQuery(
              "select j.status from BatchJob j where j.project.id = :projectId and j.type = :type",
            ).setParameter("projectId", testData.project.id)
            .setParameter("type", BatchJobType.AI_REVIEW_DEMOTION)
            .resultList as List<BatchJobStatus>
        statuses.assert.isNotEmpty()
        statuses.all { it == BatchJobStatus.SUCCESS }.assert.isTrue()
      }
    }
  }

  private fun demotionJobCount(): Long =
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createQuery(
          "select count(j) from BatchJob j where j.project.id = :projectId and j.type = :type",
          java.lang.Long::class.java,
        ).setParameter("projectId", testData.project.id)
        .setParameter("type", BatchJobType.AI_REVIEW_DEMOTION)
        .singleResult
        .toLong()
    }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)?.also { it.rule?.id }
    }

  private fun translation(id: Long) =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(id)
    }
}
