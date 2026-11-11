package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewActionService
import io.tolgee.ee.service.aiReview.AiReviewEligibility
import io.tolgee.ee.service.aiReview.AiReviewUpdate
import io.tolgee.exceptions.FailedDependencyException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.SystemActor
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventPublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.security.MessageDigest

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewJobTest : AbstractSpringTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var aiReviewActionService: AiReviewActionService

  @Autowired
  @MockitoBean
  private lateinit var openaiApiService: OpenaiApiService

  @Autowired
  @MockitoBean
  private lateinit var websocketEventPublisher: WebsocketEventPublisher

  lateinit var testData: AiReviewTestData

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
    machineTranslationProperties.freeCreditsAmount = -1
    clearInvocations(openaiApiService)
    whenever(openaiApiService.defaultAttempts()).thenReturn(listOf(30))
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = null
    llmProperties.enabled = false
    llmProperties.providers = mutableListOf()
    machineTranslationProperties.freeCreditsAmount = -1
  }

  @Test
  fun `Active pass sets REVIEWED, keeps auto and mtProvider, row APPROVED with its snapshot`() {
    testData.deTranslation.auto = true
    testData.deTranslation.mtProvider = MtServiceType.GOOGLE
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubPass()

    runJob(testData.deTranslation.id)

    val translation = translation(testData.deTranslation.id)
    translation.state.assert.isEqualTo(TranslationState.REVIEWED)
    translation.auto.assert.isTrue()
    translation.mtProvider.assert.isEqualTo(MtServiceType.GOOGLE)
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.score.assert.isEqualTo(100)
    row.threshold.assert.isEqualTo(85)
    row.rule!!
      .id.assert
      .isEqualTo(testData.rule.id)
    row.ruleMode.assert.isEqualTo(AiReviewRuleMode.ACTIVE)
    row.model.assert.isEqualTo("gpt-test")
    row.providerName.assert.isEqualTo("server-provider")
    row.promptName.assert.isEqualTo("Default review prompt")
    row.batchJobId.assert.isNull()
    row.reviewedAt.assert.isNotNull()
  }

  @Test
  fun `Active fail keeps TRANSLATED, row BELOW_THRESHOLD with issues`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubFail()

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    row.score.assert.isEqualTo(FAILING_SCORE)
    row.issues.assert.hasSize(1)
    row.issues[0]
      .severity.assert
      .isEqualTo("major")
  }

  @Test
  fun `Score only pass never changes state`() {
    testData.rule.mode = AiReviewRuleMode.SCORE_ONLY
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubPass()

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.SCORE_ONLY_PASS)
  }

  @Test
  fun `Score only fail never changes state`() {
    testData.rule.mode = AiReviewRuleMode.SCORE_ONLY
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubFail()

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.SCORE_ONLY_FAIL)
  }

  @Test
  fun `parse failure on a first review gives FAILED PARSE_ERROR and no state change`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubResponse("this is not json at all")

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.FAILED)
    row.failureReason.assert.isEqualTo(AiReviewFailureReason.PARSE_ERROR)
    row.score.assert.isNull()
  }

  @Test
  fun `out of credits gives FAILED OUT_OF_CREDITS without an LLM call`() {
    machineTranslationProperties.freeCreditsAmount = 0
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.FAILED)
    row.failureReason.assert.isEqualTo(AiReviewFailureReason.OUT_OF_CREDITS)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `a missing provider gives FAILED NO_PROVIDER`() {
    val prompt =
      testData.projectBuilder
        .addPrompt {
          name = "Broken provider"
          providerName = "missing-provider"
          type = PromptType.REVIEW
        }.self
    testData.rule.prompt = prompt
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.FAILED)
    row.failureReason.assert.isEqualTo(AiReviewFailureReason.NO_PROVIDER)
  }

  @Test
  fun `a broken prompt template gives FAILED TEMPLATE_ERROR`() {
    val prompt =
      testData.projectBuilder
        .addPrompt {
          name = "Broken template"
          providerName = "server-provider"
          type = PromptType.REVIEW
          template = "{{#if}}"
        }.self
    testData.rule.prompt = prompt
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.FAILED)
    row.failureReason.assert.isEqualTo(AiReviewFailureReason.TEMPLATE_ERROR)
  }

  @Test
  fun `a provider error gives FAILED PROVIDER_ERROR`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    whenever(openaiApiService.translate(any(), any(), any()))
      .thenThrow(FailedDependencyException(Message.LLM_PROVIDER_ERROR, listOf("boom")))

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.FAILED)
    row.failureReason.assert.isEqualTo(AiReviewFailureReason.PROVIDER_ERROR)
  }

  @Test
  fun `the claim guard lets only one job claim a row`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    val textHash = review(testData.deTranslation.id)!!.textHash

    claim(jobId = 1, textHash).assert.isEqualTo(1)
    claim(jobId = 2, textHash).assert.isEqualTo(0)
    claim(jobId = 1, textHash).assert.isEqualTo(1)
  }

  private fun claim(
    jobId: Long,
    textHash: String,
  ): Int =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.claimForJob(
        jobId = jobId,
        translationId = testData.deTranslation.id,
        textHash = textHash,
      )
    }

  @Test
  fun `failed re-review of an APPROVED row keeps everything and sets the last failure`() {
    approvedReviewedTranslation(reReviewRequested = true)
    save()
    stubResponse("broken")

    runJob(testData.deTranslation.id)

    val translation = translation(testData.deTranslation.id)
    translation.state.assert.isEqualTo(TranslationState.REVIEWED)
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.score.assert.isEqualTo(APPROVED_SCORE)
    row.lastFailureReason.assert.isEqualTo(AiReviewFailureReason.PARSE_ERROR)
    row.lastFailedAt.assert.isNotNull()
    row.reReviewRequested.assert.isFalse()
  }

  @Test
  fun `successful re-review clears the last failure`() {
    approvedReviewedTranslation(reReviewRequested = true) {
      lastFailureReason = AiReviewFailureReason.PARSE_ERROR
    }
    save()
    stubPass()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.score.assert.isEqualTo(100)
    row.lastFailureReason.assert.isNull()
    row.lastFailedAt.assert.isNull()
  }

  @Test
  fun `re-review of an approval with a stricter Score only rule keeps the approval`() {
    approvedReviewedTranslation(reReviewRequested = true)
    testData.addRule {
      name = "Strict score only"
      mode = AiReviewRuleMode.SCORE_ONLY
      threshold = 100
      tagIn = mutableListOf("ui")
    }
    save()
    stubResponse(issuesResponse(points = 10))

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.score.assert.isEqualTo(APPROVED_SCORE)
    row.threshold.assert.isEqualTo(85)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `re-review of an approval with no Active rule is skipped and clears the request`() {
    testData.rule.mode = AiReviewRuleMode.SCORE_ONLY
    approvedReviewedTranslation(reReviewRequested = true)
    save()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.reReviewRequested.assert.isFalse()
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `an AI-managed PENDING row with no Active rule becomes FAILED and leaves the queue`() {
    testData.rule.mode = AiReviewRuleMode.SCORE_ONLY
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    save()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.FAILED)
    row.reReviewRequested.assert.isFalse()
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `re-review of an APPROVED row that now fails sets TRANSLATED and BELOW_THRESHOLD`() {
    approvedReviewedTranslation(reReviewRequested = true)
    save()
    stubFail()

    runJob(testData.deTranslation.id)

    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    row.score.assert.isEqualTo(FAILING_SCORE)
  }

  @Test
  fun `under Protect reviewed a failing re-review keeps REVIEWED and auto`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    approvedReviewedTranslation(reReviewRequested = true)
    save()
    stubFail()

    runJob(testData.deTranslation.id)

    val translation = translation(testData.deTranslation.id)
    translation.state.assert.isEqualTo(TranslationState.REVIEWED)
    translation.auto.assert.isTrue()
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
  }

  @Test
  fun `under Protect reviewed a failing re-review of human-written text stays AI-managed and sends its badge`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    approvedReviewedTranslation(reReviewRequested = true)
    testData.deTranslation.auto = false
    save()
    stubFail()

    runJob(testData.deTranslation.id)

    executeInNewTransaction(platformTransactionManager) {
      val translation = translationService.get(testData.deTranslation.id)
      val row = translationAiReviewRepository.findByTranslationId(translation.id)!!
      row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
      translation.state.assert.isEqualTo(TranslationState.REVIEWED)
      AiReviewEligibility.isAiManagedReview(translation, row).assert.isTrue()
    }
    val destination = argumentCaptor<String>()
    val event = argumentCaptor<WebsocketEvent>()
    verify(websocketEventPublisher, atLeastOnce()).invoke(destination.capture(), event.capture())
    val index = destination.allValues.lastIndexOf("/projects/${testData.project.id}/ai-review-updated")

    @Suppress("UNCHECKED_CAST")
    val updates = event.allValues[index].data as List<AiReviewUpdate>
    updates
      .single()
      .status.assert
      .isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
  }

  @Test
  fun `under Protect reviewed a PENDING row on a Reviewed auto text is reviewed with Active rules`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubPass()

    runJob(testData.deTranslation.id)

    val translation = translation(testData.deTranslation.id)
    translation.state.assert.isEqualTo(TranslationState.REVIEWED)
    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `skips non-claimable rows without calling the LLM`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.OVERRIDDEN, 95)
    save()

    runJob(testData.deTranslation.id)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.OVERRIDDEN)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `deletes the row of a human-reviewed translation without calling the LLM`() {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    review(testData.deTranslation.id).assert.isNull()
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `deletes the row when no rule matches any more`() {
    val other = testData.addKeyWithTranslations("plain.key", tags = emptyList())
    testData.addReview(other.de, AiReviewStatus.PENDING)
    save()

    runJob(other.de.self.id)

    review(other.de.self.id).assert.isNull()
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `text changed during the LLM call drops the result and reviews the new text`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    whenever(openaiApiService.translate(any(), any(), any())).thenAnswer {
      executeInNewTransaction(platformTransactionManager) {
        val translation = entityManager.find(Translation::class.java, testData.deTranslation.id)
        translation.text = "Sofort zahlen"
        entityManager.persist(translation)
      }
      PromptResult(response = PASS_RESPONSE, usage = null)
    }

    runJob(testData.deTranslation.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val row = review(testData.deTranslation.id)!!
      row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      row.textHash.assert.isEqualTo(sha256Hex("Sofort zahlen"))
    }
    translation(testData.deTranslation.id).text.assert.isEqualTo("Sofort zahlen")
    verify(openaiApiService, times(2)).translate(any(), any(), any())
  }

  @Test
  fun `two jobs on the same row call the LLM once`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubPass()

    val first = startJob(testData.deTranslation.id)
    val second = startJob(testData.deTranslation.id)
    waitForJob(first)
    waitForJob(second)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    verify(openaiApiService, times(1)).translate(any(), any(), any())
  }

  @Test
  fun `a retried chunk of the same job can claim its item again`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    var calls = 0
    whenever(openaiApiService.translate(any(), any(), any())).thenAnswer {
      calls++
      if (calls <= 3) throw LlmRateLimitedException(retryAt = currentDateProvider.date.time)
      PromptResult(response = PASS_RESPONSE, usage = null)
    }

    runJob(testData.deTranslation.id, timeout = 60_000)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.APPROVED)
  }

  @Test
  fun `key deleted while the job runs is skipped and the job succeeds`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    whenever(openaiApiService.translate(any(), any(), any())).thenAnswer {
      executeInNewTransaction(platformTransactionManager) {
        listOf(
          "delete from translation where key_id = :id",
          "delete from key_meta_tags where key_metas_id in (select id from key_meta where key_id = :id)",
          "delete from key_meta where key_id = :id",
          "delete from key where id = :id",
        ).forEach { sql ->
          entityManager
            .createNativeQuery(sql)
            .setParameter("id", testData.key.id)
            .executeUpdate()
        }
      }
      PromptResult(response = PASS_RESPONSE, usage = null)
    }

    runJob(testData.deTranslation.id)

    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(testData.deTranslation.id).assert.isNull()
    }
  }

  @Test
  fun `state change is logged with the system actor AI_REVIEWER`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubPass()

    runJob(testData.deTranslation.id)

    executeInNewTransaction(platformTransactionManager) {
      val revisions =
        entityManager
          .createQuery(
            "from ActivityRevision ar where ar.projectId = :projectId and ar.systemActor = :actor",
            io.tolgee.model.activity.ActivityRevision::class.java,
          ).setParameter("projectId", testData.project.id)
          .setParameter("actor", SystemActor.AI_REVIEWER)
          .resultList
      revisions.assert.isNotEmpty()
      revisions
        .flatMap { it.modifiedEntities }
        .any { it.entityClass == "Translation" && it.entityId == testData.deTranslation.id }
        .assert
        .isTrue()
    }
  }

  @Test
  fun `an outdated AI approval queued for a re-review keeps its row and leaves the queue`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    approvedReviewedTranslation(reReviewRequested = true)
    testData.deTranslation.outdated = true
    save()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.reReviewRequested.assert.isFalse()
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `an outdated AI-managed PENDING row becomes FAILED and stays`() {
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.deTranslation.outdated = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()

    runJob(testData.deTranslation.id)

    review(testData.deTranslation.id)!!.status.assert.isEqualTo(AiReviewStatus.FAILED)
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
    verify(openaiApiService, never()).translate(any(), any(), any())
  }

  @Test
  fun `a failed re-review keeps the rule, threshold and prompt of the kept result`() {
    val stricter = addStricterRuleThatKeepsTheApproval()
    approvedReviewedTranslation(reReviewRequested = true) {
      rule = testData.rule
      promptName = "Old prompt"
    }
    save()
    stubResponse("broken")

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.lastFailureReason.assert.isEqualTo(AiReviewFailureReason.PARSE_ERROR)
    row.rule!!
      .id.assert
      .isEqualTo(testData.rule.id)
    row.threshold.assert.isEqualTo(85)
    row.promptName.assert.isEqualTo("Old prompt")
  }

  @Test
  fun `a passing re-review of an approval does not count it again`() {
    approvedReviewedTranslation(reReviewRequested = true) { rule = testData.rule }
    save()
    stubPass()

    runJob(testData.deTranslation.id)

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.APPROVED)
    row.score.assert.isEqualTo(100)
    row.reReviewRequested.assert.isFalse()
    autoReviewedCount(testData.rule.id).assert.isEqualTo(0)
  }

  @Test
  fun `websocket event is published after the job`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING)
    save()
    stubPass()

    runJob(testData.deTranslation.id)

    val destination = argumentCaptor<String>()
    val event = argumentCaptor<WebsocketEvent>()
    verify(websocketEventPublisher, atLeastOnce()).invoke(destination.capture(), event.capture())
    val index = destination.allValues.indexOf("/projects/${testData.project.id}/ai-review-updated")
    index.assert.isGreaterThanOrEqualTo(0)
    @Suppress("UNCHECKED_CAST")
    val updates = event.allValues[index].data as List<AiReviewUpdate>
    updates.assert.hasSize(1)
    updates[0].translationId.assert.isEqualTo(testData.deTranslation.id)
    updates[0].status.assert.isEqualTo(AiReviewStatus.APPROVED)
    updates[0].score.assert.isEqualTo(100)
    updates[0].scoreOnlyRun.assert.isEqualTo(false)
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
  }

  private fun approvedReviewedTranslation(
    reReviewRequested: Boolean,
    ft: TranslationAiReview.() -> Unit = {},
  ) {
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, APPROVED_SCORE) {
      this.reReviewRequested = reReviewRequested
      this.threshold = 85
      ft(this)
    }
  }

  private fun addStricterRuleThatKeepsTheApproval(): AiReviewRule =
    testData
      .addRule {
        name = "Stricter"
        mode = AiReviewRuleMode.ACTIVE
        threshold = APPROVED_SCORE - 2
        tagIn = mutableListOf("ui")
      }.self

  private fun autoReviewedCount(ruleId: Long): Long =
    executeInNewTransaction(platformTransactionManager) {
      entityManager.find(AiReviewRule::class.java, ruleId).autoReviewedCount
    }

  private fun startJob(
    vararg translationIds: Long,
    author: UserAccount? = null,
  ): BatchJob =
    executeInNewTransaction(platformTransactionManager) {
      val projectRef = entityManager.getReference(Project::class.java, testData.project.id)
      batchJobService.startJob(
        request = AiReviewRequest(translationIds = translationIds.toList()),
        project = projectRef,
        author = author,
        type = BatchJobType.AI_REVIEW,
        isHidden = true,
      )
    }

  private fun runJob(
    vararg translationIds: Long,
    timeout: Long = 30_000,
  ) {
    val job = startJob(*translationIds)
    waitForJob(job, timeout)
  }

  private fun waitForJob(
    job: BatchJob,
    timeout: Long = 30_000,
  ) {
    waitForNotThrowing(pollTime = 100, timeout = timeout) {
      batchJobService
        .getJobDtoNoCache(job.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)?.also {
        it.issues.size
        it.rule?.id
      }
    }

  private fun translation(id: Long): Translation =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(id)
    }

  private fun stubResponse(response: String) {
    whenever(openaiApiService.translate(any(), any(), any()))
      .thenReturn(PromptResult(response = response, usage = null))
  }

  private fun stubPass() = stubResponse(PASS_RESPONSE)

  private fun stubFail() = stubResponse(issuesResponse(points = 100 - FAILING_SCORE))

  private fun issuesResponse(points: Int) =
    """{"issues": [{"severity": "major", "category": "accuracy", "message": "Wrong.", "points": $points}]}"""

  companion object {
    private const val PASS_RESPONSE = """{"issues": []}"""

    private const val APPROVED_SCORE = 90

    private const val FAILING_SCORE = 50
  }

  private fun sha256Hex(text: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(text.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
}
