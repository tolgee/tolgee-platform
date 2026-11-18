package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.OrganizationLlmProviderCreditsTestData
import io.tolgee.development.testDataBuilder.data.OrganizationLlmProviderCreditsTestData.Companion.ORGANIZATION_PROVIDER
import io.tolgee.development.testDataBuilder.data.OrganizationLlmProviderCreditsTestData.Companion.TOLGEE_TYPE_ORGANIZATION_PROVIDER
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.LlmProviderType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.ResultActions

class OrganizationLlmProviderCreditsTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: OrganizationLlmProviderCreditsTestData

  private var llmEnabledBefore: Boolean? = null
  private lateinit var llmProvidersBefore: MutableList<LlmProperties.LlmProvider>
  private var fakeLlmProvidersBefore = false

  @BeforeEach
  fun setup() {
    testData = OrganizationLlmProviderCreditsTestData()
    testDataService.saveTestData(testData.root)
    llmEnabledBefore = llmProperties.enabled
    llmProvidersBefore = llmProperties.providers
    fakeLlmProvidersBefore = internalProperties.fakeLlmProviders
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          type = LlmProviderType.OPENAI,
          tokenPriceInCreditsInput = 2.0,
          tokenPriceInCreditsOutput = 2.0,
          apiUrl = "http://test.com",
        ),
        LlmProperties.LlmProvider(
          name = SERVER_PROVIDER_WITHOUT_PRICE,
          type = LlmProviderType.OPENAI,
          apiUrl = "http://test.com",
        ),
      )
    internalProperties.fakeLlmProviders = true
    initMachineTranslationProperties(0)
    this.userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    machineTranslationProperties.freeCreditsAmount = -1
    llmProperties.enabled = llmEnabledBefore
    llmProperties.providers = llmProvidersBefore
    internalProperties.fakeLlmProviders = fakeLlmProvidersBefore
  }

  @Test
  fun `runs prompt on organization provider when out of credits`() {
    runPrompt(ORGANIZATION_PROVIDER).andIsOk.andAssertThatJson {
      node("result").isString.contains("response from: $ORGANIZATION_PROVIDER")
      node("price").isEqualTo(0)
    }
  }

  @Test
  fun `does not run prompt on server provider when out of credits`() {
    runPrompt("default").andIsBadRequest.andHasErrorMessage(Message.OUT_OF_CREDITS)
  }

  @Test
  fun `runs prompt on server provider without price when out of credits`() {
    runPrompt(SERVER_PROVIDER_WITHOUT_PRICE).andIsOk.andAssertThatJson {
      node("price").isEqualTo(0)
    }
  }

  @Test
  fun `does not run prompt on organization provider reporting its own price when out of credits`() {
    runPrompt(TOLGEE_TYPE_ORGANIZATION_PROVIDER).andIsBadRequest.andHasErrorMessage(Message.OUT_OF_CREDITS)
  }

  @Test
  fun `machine translates with organization provider when out of credits`() {
    suggest(setOf(MtServiceType.PROMPT)).andIsOk.andAssertThatJson {
      node("result.PROMPT.output").isEqualTo("response from: $ORGANIZATION_PROVIDER")
    }
  }

  @Test
  fun `does not machine translate with a paid service next to organization provider when out of credits`() {
    suggest(setOf(MtServiceType.PROMPT, MtServiceType.GOOGLE))
      .andIsBadRequest
      .andHasErrorMessage(Message.OUT_OF_CREDITS)
  }

  private fun runPrompt(provider: String): ResultActions {
    return performAuthPost(
      "/v2/projects/${testData.project.id}/prompts/run",
      PromptRunDto(
        template = null,
        keyId = testData.key.id,
        targetLanguageId = testData.czech.id,
        provider = provider,
        basicPromptOptions = null,
      ),
    )
  }

  private fun suggest(services: Set<MtServiceType>): ResultActions {
    return performAuthPost(
      "/v2/projects/${testData.project.id}/suggest/machine-translations",
      SuggestRequestDto(
        keyId = testData.key.id,
        targetLanguageId = testData.czech.id,
        services = services,
      ),
    )
  }

  companion object {
    private const val SERVER_PROVIDER_WITHOUT_PRICE = "server-provider-without-price"
  }
}
