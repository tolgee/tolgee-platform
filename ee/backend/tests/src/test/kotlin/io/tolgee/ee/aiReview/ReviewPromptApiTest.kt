package io.tolgee.ee.aiReview

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class ReviewPromptApiTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: AiReviewTestData

  private val projectUrl get() = "/v2/projects/${testData.project.id}"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(LlmProperties.LlmProvider(type = LlmProviderType.OPENAI, apiUrl = "http://test.com"))
    testData = AiReviewTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
    llmProperties.enabled = false
    llmProperties.providers = mutableListOf()
  }

  @Test
  fun `creates a REVIEW prompt with criteria`() {
    val body =
      mapOf(
        "name" to "Legal review",
        "providerName" to "default",
        "type" to "REVIEW",
        "reviewCriteria" to "Legal terms must stay exact.",
      )
    performAuthPost("$projectUrl/prompts", body).andIsOk.andAssertThatJson {
      node("type").isEqualTo("REVIEW")
      node("reviewCriteria").isEqualTo("Legal terms must stay exact.")
      node("revision").isEqualTo(0)
    }
  }

  @Test
  fun `prompt type defaults to TRANSLATE`() {
    performAuthPost("$projectUrl/prompts", mapOf("name" to "Plain", "providerName" to "default"))
      .andIsOk
      .andAssertThatJson { node("type").isEqualTo("TRANSLATE") }
  }

  @Test
  fun `creating a REVIEW prompt without AI_REVIEW returns FEATURE_NOT_ENABLED`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthPost("$projectUrl/prompts", mapOf("name" to "R", "providerName" to "default", "type" to "REVIEW"))
      .andIsBadRequest
      .andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  fun `type cannot change on update`() {
    val body = mapOf("name" to "Now translate", "providerName" to "default", "type" to "TRANSLATE")
    performAuthPut("$projectUrl/prompts/${testData.reviewPrompt.id}", body)
      .andIsBadRequest
      .andHasErrorMessage(Message.PROMPT_TYPE_CANNOT_BE_CHANGED)
  }

  @Test
  fun `updating keeps the review criteria`() {
    val body =
      mapOf(
        "name" to "Brand voice review",
        "providerName" to "default",
        "type" to "REVIEW",
        "reviewCriteria" to "Use informal French.",
      )
    performAuthPut("$projectUrl/prompts/${testData.reviewPrompt.id}", body).andIsOk.andAssertThatJson {
      node("reviewCriteria").isEqualTo("Use informal French.")
    }
  }

  @Test
  fun `list filters by type`() {
    performAuthGet("$projectUrl/prompts?type=REVIEW").andIsOk.andAssertThatJson {
      node("_embedded.prompts").isArray.hasSize(1)
      node("_embedded.prompts[0].id").isEqualTo(testData.reviewPrompt.id)
    }
    performAuthGet("$projectUrl/prompts?type=TRANSLATE").andIsOk.andAssertThatJson {
      node("_embedded.prompts").isArray.hasSize(1)
      node("_embedded.prompts[0].id").isEqualTo(testData.translatePrompt.id)
    }
  }

  @Test
  fun `list without type returns both`() {
    performAuthGet("$projectUrl/prompts").andIsOk.andAssertThatJson {
      node("_embedded.prompts").isArray.hasSize(2)
    }
  }

  @Test
  fun `list without AI_REVIEW hides review prompts`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet("$projectUrl/prompts").andIsOk.andAssertThatJson {
      node("_embedded.prompts").isArray.hasSize(1)
      node("_embedded.prompts[0].id").isEqualTo(testData.translatePrompt.id)
    }
  }

  @Test
  fun `getting a review prompt without AI_REVIEW returns FEATURE_NOT_ENABLED`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthGet("$projectUrl/prompts/${testData.reviewPrompt.id}")
      .andIsBadRequest
      .andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  fun `MT settings reject a REVIEW prompt`() {
    val serviceInfo = mapOf("serviceType" to "PROMPT", "promptId" to testData.reviewPrompt.id)
    val settings =
      mapOf(
        "settings" to
          listOf(
            mapOf(
              "targetLanguageId" to testData.german.id,
              "primaryServiceInfo" to serviceInfo,
              "enabledServicesInfo" to listOf(serviceInfo),
            ),
          ),
      )
    performAuthPut("$projectUrl/machine-translation-service-settings", settings)
      .andIsBadRequest
      .andHasErrorMessage(Message.REVIEW_PROMPT_CANNOT_TRANSLATE)
  }

  @Test
  fun `set-default-prompt rejects a REVIEW prompt`() {
    performAuthPut(
      "$projectUrl/machine-translation-service-settings/set-default-prompt/${testData.reviewPrompt.id}",
      null,
    ).andIsBadRequest.andHasErrorMessage(Message.REVIEW_PROMPT_CANNOT_TRANSLATE)
  }
}
