package io.tolgee.api.v2.controllers.translationSuggestionController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.EeSubscriptionInfoProvider
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.model.Language
import io.tolgee.service.machineTranslation.MtService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean

class TranslationSuggestionControllerStreamingTest : ProjectAuthControllerTest("/v2/projects/") {
  @Suppress("LateinitVarOverridesLateinitVar")
  @SpyBean
  @Autowired
  override lateinit var mtService: MtService

  lateinit var testData: BaseTestData

  lateinit var czechLanguage: Language
  lateinit var hindiLanguage: Language

  @MockBean
  @Autowired
  private lateinit var eeSubscriptionInfoProvider: EeSubscriptionInfoProvider

  @BeforeEach
  fun setup() {
    initMachineTranslationProperties(
      freeCreditsAmount = -1,
      enabledServices = setOf(MtServiceType.GOOGLE, MtServiceType.PROMPT, MtServiceType.DEEPL),
    )
    Mockito.clearInvocations(mtService)
    internalProperties.fakeMtProviders = true

    testData =
      BaseTestData().apply {
        czechLanguage = projectBuilder.addCzech().self
        hindiLanguage = projectBuilder.addHindi().self
      }

    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    whenever(eeSubscriptionInfoProvider.isSubscribed()).thenAnswer { true }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it works`() {
    val response =
      performProjectAuthPost(
        "suggest/machine-translations-streaming",
        mapOf(
          "targetLanguageId" to czechLanguage.id,
          "baseText" to "text",
        ),
      ).andDo {
        it.asyncResult
      }.andReturn().response.contentAsString

    response.split("\n").filter { it.isNotBlank() }.map {
      jacksonObjectMapper().readValue(it, Any::class.java)
    }.assert.hasSize(4)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns json error on 400`() {
    val response =
      performProjectAuthPost(
        "suggest/machine-translations-streaming",
        mapOf(
          "targetLanguageId" to czechLanguage.id,
          "keyId" to -1,
        ),
      ).andPrettyPrint.andAssertThatJson.isEqualTo(
        """
        {
          "code" : "key_not_found",
          "params" : null
        }
        """.trimIndent(),
      )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it does not return unsupporting services`() {
    val response =
      performProjectAuthPost(
        "suggest/machine-translations-streaming",
        mapOf(
          "targetLanguageId" to hindiLanguage.id,
          "baseText" to "text",
        ),
      ).andDo {
        it.asyncResult
      }.andReturn().response.contentAsString

    response.split("\n").filter { it.isNotBlank() }.map {
      jacksonObjectMapper().readValue(it, Any::class.java)
    }.assert.hasSize(3)

    response.assert.doesNotContain("DEEPL")
  }
}
