package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiPromptCustomizationTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AiPromptCustomizationControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private lateinit var testData: AiPromptCustomizationTestData

  @BeforeEach
  fun setup() {
    testData = AiPromptCustomizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_PROMPT_CUSTOMIZATION)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get project prompt customization`() {
    performProjectAuthGet("ai-prompt-customization")
      .andIsOk
      .andAssertThatJson {
        node("description").isEqualTo(testData.project.aiTranslatorPromptDescription)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `set project prompt customization`() {
    performProjectAuthPut(
      "ai-prompt-customization",
      mapOf("description" to "new description"),
    ).andIsOk.andAssertThatJson {
      node("description").isEqualTo("new description")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `set project prompt customization fails when feature is not enabled`() {
    enabledFeaturesProvider.forceEnabled = emptySet()
    performProjectAuthPut(
      "ai-prompt-customization",
      mapOf(
        "description" to "new description",
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get language prompt customizations`() {
    performProjectAuthGet("language-ai-prompt-customizations")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.promptCustomizations") {
          isArray.hasSize(2)
          node("[0].description").isEqualTo(testData.czech.aiTranslatorPromptDescription)
          node("[1].description").isEqualTo(testData.french.aiTranslatorPromptDescription)
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `set language prompt customization`() {
    performProjectAuthPut(
      "languages/${testData.czech.id}/ai-prompt-customization",
      mapOf(
        "description" to "new description",
      ),
    ).andIsOk.andAssertThatJson {
      node("description").isEqualTo("new description")
    }
  }
}
