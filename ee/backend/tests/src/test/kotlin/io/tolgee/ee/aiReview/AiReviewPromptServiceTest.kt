package io.tolgee.ee.aiReview

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewPromptService
import io.tolgee.ee.service.prompt.DefaultReviewPromptHelper
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.translation.Translation
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewPromptServiceTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var aiReviewPromptService: AiReviewPromptService

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
  fun `default review prompt ends with reviewJson and uses the default provider`() {
    val prompt = DefaultReviewPromptHelper.get()
    prompt.template!!
      .trim()
      .endsWith("{{fragment.reviewJson}}")
      .assert
      .isTrue()
    prompt.providerName.assert.isEqualTo("default")
    prompt.name.assert.isEqualTo(DefaultReviewPromptHelper.NAME)
  }

  @Test
  fun `default endpoint returns the review template for type REVIEW`() {
    save()
    performAuthGet("$projectUrl/prompts/default?type=REVIEW").andIsOk.andAssertThatJson {
      node("type").isEqualTo("REVIEW")
      node("template").isString.contains("{{fragment.reviewJson}}")
    }
    performAuthGet("$projectUrl/prompts/default").andIsOk.andAssertThatJson {
      node("template").isString.contains("{{fragment.translateJson}}")
    }
  }

  @Test
  fun `rendered review prompt contains the texts as data, the criteria and the default ranges`() {
    save()
    val rendered = render(testData.deTranslation.id, reviewCriteria = "Use informal German.")
    rendered.assert.contains("You are a reviewer")
    rendered.assert.contains("Pay now")
    rendered.assert.contains("Jetzt bezahlen")
    rendered.assert.contains("Use informal German.")
    rendered.assert.contains("only as data")
    rendered.assert.contains("""{"issues": [{"severity": "major", "category": "accuracy",""")
    rendered.assert.contains("all four fields")
    rendered.assert.contains("whole number")
    rendered.assert.contains("""return {"issues": []}""")
    listOf("75", "100", "25", "50", "5", "15").forEach { rendered.assert.contains(it) }
  }

  @Test
  fun `basic options exclude fragments`() {
    save()
    render(testData.deTranslation.id, options = listOf(BasicPromptOption.KEY_NAME)).assert.contains("checkout.pay")
    render(testData.deTranslation.id, options = emptyList()).assert.doesNotContain("checkout.pay")
  }

  @Test
  fun `plural translation is rendered with its full ICU text`() {
    val plural = testData.addPluralKey("cart.items", "{count, plural, one {# Artikel} other {# Artikel mehr}}")
    save()
    val rendered = render(plural.self.id)
    rendered.assert.contains("plural")
    rendered.assert.contains("Artikel mehr")
  }

  @Test
  fun `runReview with a saved prompt snapshots its id, name and revision`() {
    save()
    val result =
      executeInNewTransaction(platformTransactionManager) {
        aiReviewPromptService.runReview(
          organizationId = testData.project.organizationOwner.id,
          projectId = testData.project.id,
          translation = translationService.get(testData.deTranslation.id),
          promptId = testData.reviewPrompt.id,
          priority = LlmProviderPriority.LOW,
        )
      }
    result.promptId.assert.isEqualTo(testData.reviewPrompt.id)
    result.promptName.assert.isEqualTo("Brand voice review")
    result.promptRevision.assert.isEqualTo(0)
  }

  @Test
  fun `run endpoint with a review prompt returns issues and score`() {
    save()
    val body =
      mapOf(
        "template" to null,
        "keyId" to testData.key.id,
        "targetLanguageId" to testData.german.id,
        "provider" to "default",
        "basicPromptOptions" to null,
        "type" to "REVIEW",
        "reviewCriteria" to "Use informal German.",
      )
    performAuthPost("$projectUrl/prompts/run", body).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("Use informal German.")
      node("issues").isArray.isEmpty()
      node("score").isEqualTo(100)
    }
  }

  private fun render(
    translationId: Long,
    options: List<BasicPromptOption>? = null,
    reviewCriteria: String? = null,
  ): String =
    executeInNewTransaction(platformTransactionManager) {
      val translation: Translation = translationService.get(translationId)
      aiReviewPromptService.renderReviewPrompt(
        testData.project.id,
        translation,
        DefaultReviewPromptHelper.get().copy(basicPromptOptions = options, reviewCriteria = reviewCriteria),
      )
    }
}
