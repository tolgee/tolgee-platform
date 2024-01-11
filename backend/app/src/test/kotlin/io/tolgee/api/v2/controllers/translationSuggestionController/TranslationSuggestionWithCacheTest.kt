package io.tolgee.api.v2.controllers.translationSuggestionController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.EeSubscriptionInfoProvider
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.tolgee.EeTolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateParams
import io.tolgee.component.mtBucketSizeProvider.MtBucketSizeProvider
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.SuggestionTestData
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.test.web.servlet.ResultActions
import java.util.*

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
  ],
)
class TranslationSuggestionWithCacheTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionTestData

  @Autowired
  @MockBean
  lateinit var mtBucketSizeProvider: MtBucketSizeProvider

  @Autowired
  @MockBean
  lateinit var eeTolgeeTranslateApiService: EeTolgeeTranslateApiService

  @Autowired
  @MockBean
  lateinit var eeSubscriptionInfoProvider: EeSubscriptionInfoProvider

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  override lateinit var cacheManager: CacheManager

  lateinit var cacheMock: Cache

  lateinit var tolgeeTranslateParamsCaptor: KArgumentCaptor<TolgeeTranslateParams>

  @BeforeEach
  fun setup() {
    Mockito.clearInvocations(eeTolgeeTranslateApiService)
    setForcedDate(Date())
    initTestData()
    initMachineTranslationProperties(1000)
    initMachineTranslationMocks()
    doAnswer { true }.whenever(eeSubscriptionInfoProvider).isSubscribed()
    mockDefaultMtBucketSize(1000)
  }

  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  private fun mockDefaultMtBucketSize(size: Long) {
    whenever(mtBucketSizeProvider.getSize(anyOrNull())).thenAnswer {
      size
    }
  }

  private fun initMachineTranslationMocks() {
    tolgeeTranslateParamsCaptor = argumentCaptor()

    whenever(
      eeTolgeeTranslateApiService.translate(
        tolgeeTranslateParamsCaptor.capture(),
      ),
    ).thenAnswer {
      MtValueProvider.MtResult(
        "Translated with Tolgee Translator",
        ((it.arguments[0] as? TolgeeTranslateParams)?.text?.length ?: 0) * 100,
      )
    }
  }

  private fun initTestData() {
    testData = SuggestionTestData()
    projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `translating is optimized`() {
    mockDefaultMtBucketSize(6000)
    testData.enableTolgee()
    testData.addAiDescriptions()
    saveTestData()

    performMtRequest().andIsOk

    performMtRequest().andIsOk

    println()
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  private fun performMtRequest(services: List<MtServiceType>? = null): ResultActions {
    return performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(
        keyId = testData.beautifulKey.id,
        targetLanguageId = testData.germanLanguage.id,
        services = services?.toSet(),
      ),
    )
  }
}
