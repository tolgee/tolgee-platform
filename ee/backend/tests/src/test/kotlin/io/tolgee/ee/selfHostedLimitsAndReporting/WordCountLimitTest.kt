package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.WordCountLimitTestData
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.limits.PlanLimitExceededWordsException
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import java.util.Date

@SpringBootTest
class WordCountLimitTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

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
      autoUpgradeEnabled = true
    }
    val testData = saveTestData(initialWordCount = 99)
    editTranslation(testData, WordCountLimitTestData.wordsText(101))
  }

  @Test
  fun `throws when a translation edit pushes instance words over the limit and auto-upgrade is disabled`() {
    saveSubscription {
      includedWords = 100
      wordsLimit = 100
      autoUpgradeEnabled = false
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
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ACTIVE
        currentPeriodEnd = Date()
        enabledFeatures = Feature.entries.toTypedArray()
        lastValidCheck = Date()
        this.includedKeys = 1
        this.includedSeats = 1
        this.keysLimit = 1
        this.seatsLimit = 1
        isPayAsYouGo = false
        build(this)
      },
    )
  }

  private fun saveTestData(initialWordCount: Int): WordCountLimitTestData {
    val testData = WordCountLimitTestData(initialWordCount)
    testDataService.saveTestData(testData.root)
    return testData
  }
}
