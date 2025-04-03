package io.tolgee.ee

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.SuggestionTestData
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

class EeTolgeeTranslatorControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: SuggestionTestData

  @Autowired
  @MockBean
  private lateinit var eeTolgeeTranslateApiService: EeTolgeeTranslateApiServiceImpl

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  private fun saveTestData() {
    testData = SuggestionTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  private fun prepareSubscription() {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ACTIVE
        currentPeriodEnd = Date()
        cancelAtPeriodEnd = false
        enabledFeatures = Feature.values()
        lastValidCheck = Date()
      },
    )
  }

  @AfterEach
  fun after() {
    internalProperties.fakeMtProviders = true
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it translates`() {
    saveTestData()
    prepareSubscription()
    internalProperties.fakeMtProviders = false

    whenever(
      eeTolgeeTranslateApiService.translate(any()),
    ).thenAnswer {
      MtValueProvider.MtResult(
        "Translated with Tolgee Translator",
        ((it.arguments[0] as? LLMParams)?.text?.length ?: 0) * 100,
        "OMG!",
      )
    }

    performProjectAuthPost(
      "suggest/machine-translations",
      mapOf("baseText" to "Yupee", "targetLanguageId" to testData.germanLanguage.id),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("result") {
        node("TOLGEE") {
          node("output").isEqualTo("Translated with Tolgee Translator")
        }
      }

      Mockito.mockingDetails(eeTolgeeTranslateApiService).invocations.assert.hasSize(1)
    }
  }
}
