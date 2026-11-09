package io.tolgee.ee.aiReview

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.aiReview.AiReviewActivityListener
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRunService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.SystemActor
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewRunTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var aiReviewActivityListener: AiReviewActivityListener

  @Autowired
  private lateinit var aiReviewRunService: AiReviewRunService

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          name = "server-provider",
          type = LlmProviderType.OPENAI,
          apiUrl = "http://test.com",
          model = "gpt-test",
        ),
      )
    internalProperties.fakeLlmProviders = true
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
    llmProperties.enabled = false
    llmProperties.providers = mutableListOf()
    internalProperties.fakeLlmProviders = false
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `a rule backlog run reviews the backlog with a visible job, the user as author and the system actor`() {
    val plain = testData.addKeyWithTranslations("plain.key", tags = emptyList())
    save()

    performAuthPost("$projectUrl/ai-review/run", runBody(ruleId = testData.rule.id)).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      review(testData.frTranslation.id)?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    }
    review(plain.de.self.id).assert.isNull()

    executeInNewTransaction(platformTransactionManager) {
      val job =
        entityManager
          .createQuery(
            "from BatchJob j where j.project.id = :projectId and j.type = :type",
            BatchJob::class.java,
          ).setParameter("projectId", testData.project.id)
          .setParameter("type", BatchJobType.AI_REVIEW)
          .singleResult
      job.hidden.assert.isFalse()
      job.author!!
        .id.assert
        .isEqualTo(testData.user.id)

      val revisions =
        entityManager
          .createQuery(
            "from ActivityRevision ar where ar.projectId = :projectId and ar.systemActor = :actor",
            io.tolgee.model.activity.ActivityRevision::class.java,
          ).setParameter("projectId", testData.project.id)
          .setParameter("actor", SystemActor.AI_REVIEWER)
          .resultList
      revisions.any { it.authorId == testData.user.id }.assert.isTrue()
    }
  }

  @Test
  fun `a run merges the review activity into one visible AI_REVIEW revision`() {
    save()

    performAuthPost("$projectUrl/ai-review/run", runBody(ruleId = testData.rule.id)).andIsOk

    waitForNotThrowing(pollTime = 200, timeout = 30_000) {
      executeInNewTransaction(platformTransactionManager) {
        val visible =
          entityManager
            .createQuery(
              "from ActivityRevision ar where ar.projectId = :projectId " +
                "and ar.type is not null and ar.batchJobChunkExecution is null",
              io.tolgee.model.activity.ActivityRevision::class.java,
            ).setParameter("projectId", testData.project.id)
            .resultList
        val all =
          entityManager
            .createQuery(
              "from ActivityRevision ar where ar.projectId = :projectId",
              io.tolgee.model.activity.ActivityRevision::class.java,
            ).setParameter("projectId", testData.project.id)
            .resultList
            .map {
              "id=" + it.id + " type=" + it.type + " exec=" + it.batchJobChunkExecution?.id +
                " job=" + it.batchJob?.id + " actor=" + it.systemActor + " author=" + it.authorId +
                " mods=" + it.modifiedEntities.size
            }
        visible.assert.withFailMessage("all revisions: " + all).hasSize(1)
        val revision = visible.single()
        revision.type.assert.isEqualTo(io.tolgee.activity.data.ActivityType.AI_REVIEW)
        revision.systemActor.assert.isEqualTo(SystemActor.AI_REVIEWER)
        revision.authorId.assert.isEqualTo(testData.user.id)
        revision.batchJob.assert.isNotNull
        revision.modifiedEntities
          .any { it.entityClass == "Translation" }
          .assert
          .isTrue()
      }
    }
  }

  @Test
  fun `a backlog run on the e2e dataset produces only typed AI_REVIEW revisions`() {
    val e2eData =
      io.tolgee.development.testDataBuilder.data
        .AiReviewE2eTestData()
    testDataService.saveTestData(e2eData.root)
    try {
      userAccount = e2eData.user
      val projectId = e2eData.project.id

      performAuthPost(
        "/v2/projects/" + projectId + "/ai-review/run",
        runBody(ruleId = e2eData.activeRule.id),
      ).andIsOk

      waitForNotThrowing(pollTime = 300, timeout = 40_000) {
        executeInNewTransaction(platformTransactionManager) {
          val unfinished =
            entityManager
              .createQuery(
                "select count(j) from BatchJob j where j.project.id = :projectId " +
                  "and j.status in (io.tolgee.model.batch.BatchJobStatus.PENDING, " +
                  "io.tolgee.model.batch.BatchJobStatus.RUNNING)",
                java.lang.Long::class.java,
              ).setParameter("projectId", projectId)
              .singleResult
          unfinished.assert.isEqualTo(0L)

          val actorRevisions =
            entityManager
              .createQuery(
                "from ActivityRevision ar where ar.projectId = :projectId and ar.systemActor is not null",
                io.tolgee.model.activity.ActivityRevision::class.java,
              ).setParameter("projectId", projectId)
              .resultList
          val dump =
            actorRevisions.map {
              "id=" + it.id + " type=" + it.type + " exec=" + it.batchJobChunkExecution?.id +
                " job=" + it.batchJob?.id + " author=" + it.authorId +
                " mods=" + it.modifiedEntities.size
            }
          actorRevisions.assert.withFailMessage("revisions: " + dump).isNotEmpty
          actorRevisions
            .all { it.type == io.tolgee.activity.data.ActivityType.AI_REVIEW && it.batchJobChunkExecution == null }
            .assert
            .withFailMessage("revisions: " + dump)
            .isTrue()
          actorRevisions
            .any { it.authorId == e2eData.user.id }
            .assert
            .withFailMessage("revisions: " + dump)
            .isTrue()
        }
      }
    } finally {
      testDataService.cleanTestData(e2eData.root)
    }
  }

  @Test
  fun `retry failed queues the FAILED rows and failing approvals of the rule only`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.FAILED) {
      rule = testData.rule
      failureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.APPROVED, 90) {
      rule = testData.rule
      lastFailureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
    val otherRule =
      testData
        .addRule {
          name = "Other"
          mode = AiReviewRuleMode.ACTIVE
          threshold = 85
          tagIn = mutableListOf("other")
        }.self
    val otherKey = testData.addKeyWithTranslations("other.key", tags = listOf("other"))
    testData.addReview(otherKey.de, AiReviewStatus.FAILED) {
      rule = otherRule
      failureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
    save()

    performAuthPost(
      "$projectUrl/ai-review/run",
      runBody(ruleId = testData.rule.id, failedOnly = true),
    ).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      review(testData.deTranslation.id)?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      val fr = review(testData.frTranslation.id)!!
      fr.lastFailureReason.assert.isNull()
    }
    review(otherKey.de.self.id)!!.status.assert.isEqualTo(AiReviewStatus.FAILED)
  }

  @Test
  fun `the preview skips translations with open QA issues`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.QA_CHECKS)
    testData.project.useQaChecks = true
    testData.rule.skipOpenQaIssues = true
    testData.deTranslation.qaChecksStale = false
    testData.deTranslationBuilder.addQaIssue { state = QaIssueState.OPEN }
    save()

    performAuthPost(
      "$projectUrl/ai-review/run/preview",
      runBody(keyIds = listOf(testData.key.id), languageIds = listOf(testData.german.id)),
    ).andIsOk.andAssertThatJson {
      node("toReview").isEqualTo(0)
      node("skipped.OPEN_QA_ISSUES").isEqualTo(1)
    }
  }

  @Test
  fun `the preview counts every skip reason`() {
    val withResult = testData.addKeyWithTranslations("k.result")
    testData.addReview(withResult.de, AiReviewStatus.APPROVED, 90)
    val queued = testData.addKeyWithTranslations("k.queued")
    testData.addReview(queued.de, AiReviewStatus.PENDING)
    val humanReviewed = testData.addKeyWithTranslations("k.human")
    humanReviewed.de.self.state = TranslationState.REVIEWED
    val noRule = testData.addKeyWithTranslations("k.norule", tags = emptyList())
    testData.addRule {
      name = "Legal never"
      never = true
      tagIn = mutableListOf("legal")
    }
    val never = testData.addKeyWithTranslations("k.never", tags = listOf("legal"))
    val outdated = testData.addKeyWithTranslations("k.outdated")
    outdated.de.self.outdated = true
    val empty = testData.addKeyWithTranslations("k.empty", deText = "")
    val confirmed = testData.addKeyWithTranslations("k.confirmed")
    testData.addReview(confirmed.de, AiReviewStatus.CONFIRMED, 95)
    val rejected = testData.addKeyWithTranslations("k.rejected")
    testData.addReview(rejected.de, AiReviewStatus.REJECTED, 95)
    save()

    val keyIds =
      listOf(withResult, queued, humanReviewed, noRule, never, outdated, empty, confirmed, rejected)
        .map { it.key.id } + testData.key.id

    performAuthPost(
      "$projectUrl/ai-review/run/preview",
      runBody(keyIds = keyIds, languageIds = listOf(testData.german.id)),
    ).andIsOk.andAssertThatJson {
      node("toReview").isEqualTo(1)
      node("dryRunOnly").isEqualTo(0)
      node("skipped.ALREADY_HAS_RESULT").isEqualTo(1)
      node("skipped.ALREADY_QUEUED").isEqualTo(1)
      node("skipped.HUMAN_REVIEWED").isEqualTo(1)
      node("skipped.NO_RULE").isEqualTo(1)
      node("skipped.NEVER").isEqualTo(1)
      node("skipped.OUTDATED").isEqualTo(1)
      node("skipped.EMPTY").isEqualTo(1)
      node("skipped.CONFIRMED_BY_HUMAN").isEqualTo(1)
      node("skipped.REJECTED_BY_HUMAN").isEqualTo(1)
    }
  }

  @Test
  fun `a Dry run winner needs no state edit, an Active winner without it is skipped`() {
    testData.addRule {
      name = "Dry french"
      mode = AiReviewRuleMode.DRY_RUN
      threshold = 85
      tagIn = mutableListOf("dry")
    }
    val dryKey = testData.addKeyWithTranslations("k.dry", tags = listOf("dry"))
    val restricted =
      testData.root
        .addUserAccount { username = "run_restricted" }
        .self
    testData.projectBuilder.addPermission {
      user = restricted
      type = null
      scopes =
        arrayOf(
          Scope.KEYS_VIEW,
          Scope.TRANSLATIONS_VIEW,
          Scope.BATCH_MACHINE_TRANSLATE,
          Scope.TRANSLATIONS_STATE_EDIT,
        )
      stateChangeLanguages = mutableSetOf(testData.german)
    }
    save()
    userAccount = restricted

    performAuthPost(
      "$projectUrl/ai-review/run/preview",
      runBody(
        keyIds = listOf(testData.key.id, dryKey.key.id),
        languageIds = listOf(testData.french.id),
      ),
    ).andIsOk.andAssertThatJson {
      node("toReview").isEqualTo(1)
      node("dryRunOnly").isEqualTo(1)
      node("skipped.NO_PERMISSION").isEqualTo(1)
    }
  }

  @Test
  fun `an Active winner on a protected branch without branch modify permission is skipped`() {
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
    val restricted =
      testData.root
        .addUserAccount { username = "run_branch_restricted" }
        .self
    testData.projectBuilder.addPermission {
      user = restricted
      type = null
      scopes =
        arrayOf(
          Scope.KEYS_VIEW,
          Scope.TRANSLATIONS_VIEW,
          Scope.BATCH_MACHINE_TRANSLATE,
          Scope.TRANSLATIONS_STATE_EDIT,
        )
    }
    save()
    userAccount = restricted

    performAuthPost(
      "$projectUrl/ai-review/run/preview",
      runBody(
        keyIds = listOf(branched.key.id),
        languageIds = listOf(testData.german.id),
      ),
    ).andIsOk.andAssertThatJson {
      node("toReview").isEqualTo(0)
      node("skipped.NO_PERMISSION").isEqualTo(1)
    }
  }

  @Test
  fun `reReview re-runs human-resolved rows too and a failure keeps the old result`() {
    llmProperties.providers = mutableListOf()
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90) {
      rule = testData.rule
    }
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.CONFIRMED, 95) {
      rule = testData.rule
    }
    save()

    performAuthPost(
      "$projectUrl/ai-review/run",
      runBody(
        keyIds = listOf(testData.key.id),
        languageIds = listOf(testData.german.id, testData.french.id),
        reReview = true,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = review(testData.deTranslation.id)!!
      de.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      de.score.assert.isEqualTo(90)
      de.lastFailureReason.assert.isEqualTo(AiReviewFailureReason.NO_PROVIDER)
      val fr = review(testData.frTranslation.id)!!
      fr.status.assert.isEqualTo(AiReviewStatus.CONFIRMED)
      fr.score.assert.isEqualTo(95)
      fr.reReviewRequested.assert.isFalse()
      fr.lastFailureReason.assert.isEqualTo(AiReviewFailureReason.NO_PROVIDER)
    }
  }

  @Test
  fun `reReview replaces human-resolved results and clears who resolved them`() {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.CONFIRMED, 60) {
      rule = testData.rule
      resolvedBy = testData.user
      resolvedAt = Date()
    }
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.REJECTED, 95) {
      rule = testData.rule
      resolvedBy = testData.user
      resolvedAt = Date()
    }
    save()

    performAuthPost(
      "$projectUrl/ai-review/run/preview",
      runBody(
        keyIds = listOf(testData.key.id),
        languageIds = listOf(testData.german.id, testData.french.id),
        reReview = true,
      ),
    ).andIsOk.andAssertThatJson {
      node("toReview").isEqualTo(2)
      node("confirmedToReReview").isEqualTo(1)
      node("rejectedToReReview").isEqualTo(1)
    }

    performAuthPost(
      "$projectUrl/ai-review/run",
      runBody(
        keyIds = listOf(testData.key.id),
        languageIds = listOf(testData.german.id, testData.french.id),
        reReview = true,
      ),
    ).andIsOk

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val de = review(testData.deTranslation.id)!!
      de.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      de.resolvedBy.assert.isNull()
      de.resolvedAt.assert.isNull()
      de.reReviewRequested.assert.isFalse()
      val fr = review(testData.frTranslation.id)!!
      fr.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      fr.resolvedBy.assert.isNull()
      fr.resolvedAt.assert.isNull()
    }
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .find(Translation::class.java, testData.deTranslation.id)
        .state.assert
        .isEqualTo(TranslationState.REVIEWED)
    }
  }

  @Test
  fun `a preview with nothing to review returns zero`() {
    testData.deTranslation.outdated = true
    save()
    performAuthPost(
      "$projectUrl/ai-review/run/preview",
      runBody(translationIds = listOf(testData.deTranslation.id)),
    ).andIsOk.andAssertThatJson {
      node("toReview").isEqualTo(0)
      node("skipped.OUTDATED").isEqualTo(1)
    }
  }

  @Test
  fun `a run or a retry with nothing to review starts no job`() {
    testData.deTranslation.outdated = true
    save()

    performAuthPost(
      "$projectUrl/ai-review/run",
      runBody(translationIds = listOf(testData.deTranslation.id)),
    ).andIsBadRequest.andHasErrorMessage(Message.AI_REVIEW_NOTHING_TO_REVIEW)
    performAuthPost("$projectUrl/translations/${testData.deTranslation.id}/ai-review/retry", null)
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_NOTHING_TO_REVIEW)

    aiReviewJobCount().assert.isZero()
  }

  @Test
  fun `a run and its preview skip the translations of a trashed key`() {
    testData.key.deletedAt = Date()
    save()
    val bodies =
      listOf(
        runBody(translationIds = listOf(testData.deTranslation.id)),
        runBody(keyIds = listOf(testData.key.id), languageIds = listOf(testData.german.id)),
      )

    bodies.forEach { body ->
      performAuthPost("$projectUrl/ai-review/run/preview", body).andIsOk.andAssertThatJson {
        node("toReview").isEqualTo(0)
      }
      performAuthPost("$projectUrl/ai-review/run", body)
        .andIsBadRequest
        .andHasErrorMessage(Message.AI_REVIEW_NOTHING_TO_REVIEW)
    }
    aiReviewJobCount().assert.isZero()
  }

  @Test
  fun `a run never reaches the translations of another project`() {
    testData.addReview(testData.otherProjectTranslationBuilder, AiReviewStatus.FAILED) {
      rule = testData.otherProjectRule
    }
    save()
    val other = testData.otherProjectTranslation
    val bodies =
      listOf(
        runBody(translationIds = listOf(other.id)),
        runBody(keyIds = listOf(testData.otherProjectKey.id), languageIds = listOf(testData.otherProjectLanguage.id)),
        runBody(ruleId = testData.otherProjectRule.id, failedOnly = true),
      )

    bodies.forEach { body ->
      performAuthPost("$projectUrl/ai-review/run/preview", body).andIsOk.andAssertThatJson {
        node("toReview").isEqualTo(0)
      }
      performAuthPost("$projectUrl/ai-review/run", body)
        .andIsBadRequest
        .andHasErrorMessage(Message.AI_REVIEW_NOTHING_TO_REVIEW)
    }

    val row = review(other.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.FAILED)
    row.reReviewRequested.assert.isFalse()
    executeInNewTransaction(platformTransactionManager) {
      translationService
        .get(other.id)
        .state.assert
        .isEqualTo(TranslationState.TRANSLATED)
    }
  }

  @Test
  fun `the popover retry checks branch modify`() {
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
    val restricted =
      testData.root
        .addUserAccount { username = "retry_restricted" }
        .self
    testData.projectBuilder.addPermission {
      user = restricted
      type = null
      scopes =
        arrayOf(
          Scope.KEYS_VIEW,
          Scope.TRANSLATIONS_VIEW,
          Scope.BATCH_MACHINE_TRANSLATE,
          Scope.TRANSLATIONS_STATE_EDIT,
        )
    }
    save()
    userAccount = restricted

    performAuthPost(
      "$projectUrl/translations/${branched.de.self.id}/ai-review/retry",
      null,
    ).andIsForbidden
  }

  @Test
  fun `cancelling a run deletes its queued rows and clears re-review requests`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING) {
      manualRunBatchJobId = 777
    }
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.APPROVED, 90) {
      reReviewRequested = true
      manualRunBatchJobId = 777
    }
    val other = testData.addKeyWithTranslations("other.key")
    testData.addReview(other.de, AiReviewStatus.PENDING)
    save()

    aiReviewActivityListener.onBatchJobCancelled(
      io.tolgee.batch.events
        .OnBatchJobCancelled(cancelledJob()),
    )

    review(testData.deTranslation.id).assert.isNull()
    val fr = review(testData.frTranslation.id)!!
    fr.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    fr.reReviewRequested.assert.isFalse()
    review(other.de.self.id)!!.status.assert.isEqualTo(AiReviewStatus.PENDING)
  }

  @Test
  fun `a run while another AI review job is active is accepted`() {
    testData.projectBuilder.addBatchJob {
      type = BatchJobType.AI_REVIEW
      status = BatchJobStatus.RUNNING
      hidden = true
    }
    save()

    performAuthPost("$projectUrl/ai-review/run", runBody(ruleId = testData.rule.id)).andIsOk
  }

  @Test
  fun `the service refuses preview and start without AI review`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    assertThatThrownBy {
      executeInNewTransaction(platformTransactionManager) {
        aiReviewRunService.preview(testData.project.id, runRequest())
      }
    }.isInstanceOf(BadRequestException::class.java)
    assertThatThrownBy {
      executeInNewTransaction(platformTransactionManager) {
        aiReviewRunService.start(
          testData.project.id,
          runRequest(),
          testData.user.id,
        )
      }
    }.isInstanceOf(BadRequestException::class.java)
  }

  private fun runRequest() =
    io.tolgee.ee.data.aiReview
      .AiReviewRunRequest(ruleId = testData.rule.id)

  private fun cancelledJob(): BatchJobDto =
    BatchJobDto(
      id = 777,
      projectId = testData.project.id,
      authorId = testData.user.id,
      target = listOf(),
      totalItems = 0,
      totalChunks = 0,
      chunkSize = 5,
      status = BatchJobStatus.CANCELLED,
      type = BatchJobType.AI_REVIEW,
      params = mapOf("handlingStuckItems" to false),
      maxPerJobConcurrency = -1,
      jobCharacter = io.tolgee.batch.JobCharacter.SLOW,
      hidden = false,
      debouncingKey = null,
    )

  private fun runBody(
    ruleId: Long? = null,
    keyIds: List<Long>? = null,
    translationIds: List<Long>? = null,
    languageIds: List<Long> = emptyList(),
    reReview: Boolean = false,
    failedOnly: Boolean = false,
  ): Map<String, Any?> =
    mapOf(
      "ruleId" to ruleId,
      "keyIds" to keyIds,
      "translationIds" to translationIds,
      "languageIds" to languageIds,
      "reReview" to reReview,
      "failedOnly" to failedOnly,
    )

  private fun aiReviewJobCount(): Long =
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createQuery(
          "select count(j) from BatchJob j where j.project.id = :projectId and j.type = :type",
          java.lang.Long::class.java,
        ).setParameter("projectId", testData.project.id)
        .setParameter("type", BatchJobType.AI_REVIEW)
        .singleResult
        .toLong()
    }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }
}
