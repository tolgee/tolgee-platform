package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.development.testDataBuilder.data.BranchedWordCountLimitTestData
import io.tolgee.development.testDataBuilder.data.WordCountLimitTestData
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.limits.PlanLimitExceededWordsException
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.service.projectExportImport.ContentReplacementScope
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate

@SpringBootTest
class WordCountLimitTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var organizationStatsService: OrganizationStatsService

  @Autowired
  private lateinit var contentReplacementScope: ContentReplacementScope

  @Autowired
  @MockitoBean
  private lateinit var restTemplate: RestTemplate

  @Autowired
  @MockitoBean
  private lateinit var billingConfProvider: PublicBillingConfProvider

  @BeforeEach
  fun initMocks() {
    val mockAny = mock<Any>()
    val mockResp = mock<ResponseEntity<Any>>()
    whenever(restTemplate.exchange(any<String>(), any(), any(), any<Class<Any>>())).thenReturn(mockResp)
    whenever(mockResp.body).thenReturn(mockAny)
    whenever(billingConfProvider()).thenReturn(PublicBillingConfigurationDTO(enabled = false))
  }

  @AfterEach
  fun clearSubscription() {
    // The ee_subscription row is a singleton, so a subscription left behind here limits every test
    // class that runs after this one.
    eeSubscriptionRepository.deleteAll()
  }

  @Test
  fun `throws when a translation edit pushes instance words over the limit`() {
    saveSubscription {
      includedWords = 100
      wordsLimit = 100
    }
    val testData = saveTestData(initialWordCount = 99)
    // The listener throws during the interceptor's flush callback, so the exception
    // surfaces wrapped in a transaction-commit exception rather than directly.
    assertThatThrownBy {
      editTranslation(testData, WordCountLimitTestData.wordsText(101))
    }.hasRootCauseInstanceOf(PlanLimitExceededWordsException::class.java)
  }

  @Test
  fun `does not throw when a translation edit brings instance words exactly to the limit`() {
    saveSubscription {
      includedWords = 100
      wordsLimit = 100
    }
    val testData = saveTestData(initialWordCount = 99)
    editTranslation(testData, WordCountLimitTestData.wordsText(100))
  }

  @Test
  fun `does not throw when unlimited (-1), e_g_ a KEYS_SEATS legacy subscription`() {
    saveSubscription {
      includedWords = -1
      wordsLimit = -1
    }
    val testData = saveTestData(initialWordCount = 99)
    editTranslation(testData, WordCountLimitTestData.wordsText(500))
  }

  @Test
  fun `does not throw when billing is enabled (cloud)`() {
    whenever(billingConfProvider()).thenReturn(PublicBillingConfigurationDTO(enabled = true))
    saveSubscription {
      includedWords = 100
      wordsLimit = 100
    }
    val testData = saveTestData(initialWordCount = 99)
    editTranslation(testData, WordCountLimitTestData.wordsText(500))
  }

  @Test
  fun `does not throw when a translation edit pushes instance words over the limit and auto-upgrade is enabled`() {
    saveSubscription {
      includedWords = 100
      wordsLimit = 100
      autoUpgradeEffective = true
    }
    val testData = saveTestData(initialWordCount = 99)
    editTranslation(testData, WordCountLimitTestData.wordsText(101))
  }

  @Test
  fun `throws when a translation edit pushes instance words over the limit and auto-upgrade is disabled`() {
    saveSubscription {
      includedWords = 100
      wordsLimit = 100
      autoUpgradeEffective = false
    }
    val testData = saveTestData(initialWordCount = 99)
    assertThatThrownBy {
      editTranslation(testData, WordCountLimitTestData.wordsText(101))
    }.hasRootCauseInstanceOf(PlanLimitExceededWordsException::class.java)
  }

  @Test
  fun `throws when auto-upgrade is absent on an old-server subscription (defaults to false, blocking)`() {
    saveSubscription {
      includedWords = 100
      wordsLimit = 100
    }
    val testData = saveTestData(initialWordCount = 99)
    assertThatThrownBy {
      editTranslation(testData, WordCountLimitTestData.wordsText(101))
    }.hasRootCauseInstanceOf(PlanLimitExceededWordsException::class.java)
  }

  @Test
  fun `does not throw on a branching project while the branch-deduplicated total stays under the limit`() {
    // The fixture is created before the licence, so building it is not itself limit-checked.
    val testData = saveBranchedTestData(defaultBranchWordCount = 1, featureBranchWordCount = 5)
    saveSubscription {
      includedKeys = -1
      keysLimit = -1
      includedWords = 10
      wordsLimit = 10
    }

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(5)

    translationService.setTranslationText(
      testData.defaultBranchTranslation,
      WordCountLimitTestData.wordsText(3),
    )

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(5)
  }

  @Test
  fun `over-counts a branching project by this transaction's own writes, and blocks on that`() {
    // The fixture is created before the licence, so building it is not itself limit-checked.
    val testData = saveBranchedTestData(defaultBranchWordCount = 1, featureBranchWordCount = 5)
    saveSubscription {
      includedKeys = -1
      keysLimit = -1
      includedWords = 5
      wordsLimit = 5
    }

    val thrown =
      catchThrowable {
        executeInNewTransaction {
          translationService.setTranslationText(
            entityManager.find(Translation::class.java, testData.defaultBranchTranslation.id),
            WordCountLimitTestData.wordsText(3),
          )
        }
      }

    val required =
      (generateSequence(thrown) { it.cause }.last() as PlanLimitExceededWordsException)
        .params
        ?.first() as Long

    // Above the true post-commit total of MAX(3, 5) = 5 — that is the over-count — but no further
    // above the pre-transaction baseline than the 3 words this transaction wrote.
    assertThat(required).isGreaterThan(5).isLessThanOrEqualTo(5 + 3)
  }

  /**
   * The running total is a raw sum of per-translation deltas, while the billed figure is
   * MAX(word_count) per key name across branches. A decrease on a branch sibling is therefore
   * subtracted from a total it never contributed to, and can cancel a real increase elsewhere in
   * the same transaction — recorded like the restore and content-replacement exemptions rather
   * than left as a surprise.
   */
  @Test
  fun `a mixed transaction on a branching project can take the instance over the ceiling unchecked`() {
    // The fixture is created before the licence, so building it is not itself limit-checked.
    val testData = saveBranchedTestData(defaultBranchWordCount = 100, featureBranchWordCount = 100)
    saveSubscription {
      includedKeys = -1
      keysLimit = -1
      includedWords = 100
      wordsLimit = 100
    }

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(100)

    executeInNewTransaction {
      // Loaded first so its dirty-check is flushed first: with the increase flushed first the
      // running total would be positive and the check would run.
      val featureBranch = entityManager.find(Translation::class.java, testData.featureBranchTranslation.id)
      val unbranched = entityManager.find(Translation::class.java, testData.unbranchedTranslation.id)

      // -100 raw, but nothing billed: the default branch still holds its own 100.
      translationService.setTranslationText(featureBranch, "")
      // +50 raw and +50 billed, cancelled by the decrease above before it is ever checked.
      translationService.setTranslationText(unbranched, WordCountLimitTestData.wordsText(50))
    }

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(150)
  }

  @Test
  fun `an instance already over the limit can still shorten a translation`() {
    val testData = saveTestData(initialWordCount = 200)
    saveSubscription {
      includedWords = 50
      wordsLimit = 50
    }

    editTranslation(testData, WordCountLimitTestData.wordsText(100))

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(100)
  }

  @Test
  fun `an instance already over the limit can still clear a translation`() {
    val testData = saveTestData(initialWordCount = 200)
    saveSubscription {
      includedWords = 50
      wordsLimit = 50
    }

    editTranslation(testData, "")

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(0)
  }

  @Test
  fun `does not block a licence that meters words without a ceiling`() {
    saveSubscription {
      includedWords = 100
      wordsLimit = -1
    }
    val testData = saveTestData(initialWordCount = 99)

    editTranslation(testData, WordCountLimitTestData.wordsText(500))

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(500)
  }

  @Test
  fun `replacing a project's content is not blocked, even though the wipe emits no events`() {
    val testData = saveTestData(initialWordCount = 200)
    saveSubscription {
      includedWords = 250
      wordsLimit = 250
    }

    // A replacement is not limit-checked at all: this write takes the instance from 200 to 300
    // against a ceiling of 250 and must still go through. The flush has to be inside the block —
    // a dirty update's event is emitted at flush, and the commit-time flush happens after the
    // marker is already down.
    executeInNewTransaction {
      contentReplacementScope.replacingContent {
        editTranslation(testData, WordCountLimitTestData.wordsText(300))
        entityManager.flush()
      }
    }

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(300)
  }

  @Test
  fun `restoring a soft-deleted key is not blocked, even when it takes the instance over the limit`() {
    val testData = saveTestData(initialWordCount = 99)
    val key =
      testData.projectBuilder.data.keys
        .first()
        .self
    executeInNewTransaction { keyService.softDeleteMultiple(listOf(key.id)) }

    saveSubscription {
      includedWords = 50
      wordsLimit = 50
    }

    executeInNewTransaction { keyService.restoreKeys(listOf(key.id), testData.projectBuilder.self.id) }

    organizationStatsService.countAllWordsOnInstance().assert.isEqualTo(99)
  }

  private fun saveBranchedTestData(
    defaultBranchWordCount: Int,
    featureBranchWordCount: Int,
  ): BranchedWordCountLimitTestData {
    val testData = BranchedWordCountLimitTestData(defaultBranchWordCount, featureBranchWordCount)
    testDataService.saveTestData(testData.root)
    return testData
  }

  private fun editTranslation(
    testData: WordCountLimitTestData,
    newText: String,
  ) {
    val translation =
      testData.projectBuilder.data.translations
        .first()
        .self
    translationService.setTranslationText(translation, newText)
  }

  private fun saveSubscription(build: EeSubscription.() -> Unit = {}) {
    eeSubscriptionRepository.save(SelfHostedSubscriptionFixture.wordPlan(build))
  }

  private fun saveTestData(initialWordCount: Int): WordCountLimitTestData {
    val testData = WordCountLimitTestData(initialWordCount)
    testDataService.saveTestData(testData.root)
    return testData
  }
}
