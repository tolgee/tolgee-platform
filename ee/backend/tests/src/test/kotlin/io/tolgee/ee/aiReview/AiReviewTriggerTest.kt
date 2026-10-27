package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.MachineTranslationRequest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
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
import java.security.MessageDigest

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewTriggerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

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
    internalProperties.fakeMtProviders = false
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `with QA off, editing a translation reviews it`() {
    save()
    editTranslation("de", "Gleich zahlen")

    waitForApproved(testData.deTranslation.id, "Gleich zahlen")
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `with QA on, the review runs after the QA job`() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.QA_CHECKS)
    testData.project.useQaChecks = true
    save()
    editTranslation("de", "Gleich zahlen")

    waitForApproved(testData.deTranslation.id, "Gleich zahlen")
    translation(testData.deTranslation.id).qaChecksStale.assert.isFalse()
  }

  @Test
  fun `machine translation output is reviewed after the job`() {
    initMachineTranslationProperties(-1)
    internalProperties.fakeMtProviders = true
    save()

    startMtJob(testData.key.id, testData.german.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val row = review(testData.deTranslation.id)
      row?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      val translation = translation(testData.deTranslation.id)
      translation.auto.assert.isTrue()
      translation.state.assert.isEqualTo(TranslationState.REVIEWED)
      row!!.textHash.assert.isEqualTo(sha256Hex(translation.text!!))
    }
  }

  @Test
  fun `editing an outdated translation clears outdated and reviews it`() {
    testData.deTranslation.outdated = true
    save()
    editTranslation("de", "Gleich zahlen")

    waitForApproved(testData.deTranslation.id, "Gleich zahlen")
    translation(testData.deTranslation.id).outdated.assert.isFalse()
  }

  @Test
  fun `setting the outdated flag alone does not review`() {
    save()
    performAuthPut(
      "$projectUrl/translations/${testData.deTranslation.id}/set-outdated-flag/true",
      null,
    ).andIsOk

    review(testData.deTranslation.id).assert.isNull()
  }

  @Test
  fun `a text change on a key with no rule creates no row`() {
    val plain = testData.addKeyWithTranslations("plain.key", tags = emptyList())
    save()
    editTranslation("de", "Anderer Text", keyName = "plain.key")

    review(plain.de.self.id).assert.isNull()
  }

  @Test
  fun `a Never rule deletes an existing row and queues nothing`() {
    testData.rule.never = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    save()
    editTranslation("de", "Gleich zahlen")

    review(testData.deTranslation.id).assert.isNull()
  }

  @Test
  fun `with no enabled rule a text change deletes even a resolved row`() {
    testData.rule.enabled = false
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.CONFIRMED, 95)
    save()
    editTranslation("de", "Gleich zahlen")

    review(testData.deTranslation.id).assert.isNull()
    aiReviewJobCount().assert.isZero()
  }

  @Test
  fun `a text change deletes the row even when AI review is off`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.CONFIRMED, 95)
    save()
    editTranslation("de", "Gleich zahlen")

    review(testData.deTranslation.id).assert.isNull()
    aiReviewJobCount().assert.isZero()
  }

  @Test
  fun `a source change deletes stale sibling rows`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.DRY_RUN_PASS, 90)
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.BELOW_THRESHOLD, 40)
    save()
    editTranslation("en", "Pay immediately")

    review(testData.deTranslation.id).assert.isNull()
    review(testData.frTranslation.id).assert.isNull()
  }

  @Test
  fun `a base language change deletes all rows except human decisions`() {
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.PENDING, null)
    // Every stored score compares against the old source, so sibling rows go too.
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.APPROVED, 90)
    val confirmed = testData.addKeyWithTranslations("confirmed_key")
    testData.addReview(confirmed.fr, AiReviewStatus.CONFIRMED, 95)
    save()

    performAuthPut(
      projectUrl,
      mapOf(
        "name" to testData.project.name,
        "baseLanguageId" to testData.german.id,
        "useNamespaces" to false,
      ),
    ).andIsOk

    review(testData.deTranslation.id).assert.isNull()
    review(testData.frTranslation.id).assert.isNull()
    review(confirmed.fr.self.id)!!.status.assert.isEqualTo(AiReviewStatus.CONFIRMED)
  }

  @Test
  fun `two edits create two jobs`() {
    save()
    editTranslation("de", "Gleich zahlen")
    editTranslation("fr", "Payer vite")

    waitForApproved(testData.deTranslation.id, "Gleich zahlen")
    waitForApproved(testData.frTranslation.id, "Payer vite")
    aiReviewJobCount().assert.isEqualTo(2)
  }

  @Test
  fun `under Protect reviewed, MT replacing an AI approval queues a review and keeps REVIEWED`() {
    initMachineTranslationProperties(-1)
    internalProperties.fakeMtProviders = true
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    save()

    startMtJob(testData.key.id, testData.german.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val translation = translation(testData.deTranslation.id)
      translation.text.assert.isNotEqualTo("Jetzt bezahlen")
      translation.state.assert.isEqualTo(TranslationState.REVIEWED)
      val row = review(testData.deTranslation.id)
      row?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      row!!.textHash.assert.isEqualTo(sha256Hex(translation.text!!))
    }
  }

  @Test
  fun `under Protect reviewed with only a Dry run rule, MT leaves no row and keeps REVIEWED`() {
    initMachineTranslationProperties(-1)
    internalProperties.fakeMtProviders = true
    testData.project.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.rule.mode = AiReviewRuleMode.DRY_RUN
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 90)
    save()

    startMtJob(testData.key.id, testData.german.id)

    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val translation = translation(testData.deTranslation.id)
      translation.text.assert.isNotEqualTo("Jetzt bezahlen")
      translation.state.assert.isEqualTo(TranslationState.REVIEWED)
      review(testData.deTranslation.id).assert.isNull()
    }
  }

  private fun editTranslation(
    languageTag: String,
    text: String,
    keyName: String = "checkout.pay",
  ) {
    performAuthPut(
      "$projectUrl/translations",
      mapOf("key" to keyName, "translations" to mapOf(languageTag to text)),
    ).andIsOk
  }

  private fun startMtJob(
    keyId: Long,
    languageId: Long,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      val projectRef = entityManager.getReference(Project::class.java, testData.project.id)
      batchJobService.startJob(
        request =
          MachineTranslationRequest().apply {
            keyIds = listOf(keyId)
            targetLanguageIds = listOf(languageId)
          },
        project = projectRef,
        author = null,
        type = BatchJobType.MACHINE_TRANSLATE,
        isHidden = true,
      )
    }
  }

  private fun waitForApproved(
    translationId: Long,
    expectedText: String,
  ) {
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      val row = review(translationId)
      row?.status.assert.isEqualTo(AiReviewStatus.APPROVED)
      row!!.textHash.assert.isEqualTo(sha256Hex(expectedText))
    }
  }

  private fun sha256Hex(text: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(text.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)
    }

  private fun translation(id: Long): Translation =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(id)
    }

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
}
