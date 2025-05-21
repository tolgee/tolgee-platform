package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.ee.development.PromptTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.LLMProviderType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PromptControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: PromptTestData

  @BeforeEach
  fun setup() {
    testData = PromptTestData()
    testDataService.saveTestData(testData.root)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LLMProperties.LLMProvider(
          type = LLMProviderType.OPENAI,
          pricePerMillionInput = 2.0,
          pricePerMillionOutput = 2.0,
        ),
      )
    internalProperties.fakeLlmProviders = true
    this.userAccount = testData.projectEditor.self
  }

  @Test
  fun `returns default prompt`() {
    performAuthGet(
      "/v2/projects/${testData.promptProject.self.id}/prompts/default",
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("default")
      node("providerName").isEqualTo("default")
    }
  }

  @Test
  fun `returns available prompts`() {
    performAuthGet(
      "/v2/projects/${testData.promptProject.self.id}/prompts",
    ).andIsOk.andAssertThatJson {
      val items = node("_embedded.prompts")
      items.isArray.hasSize(1)
      items.node("[0].name").isEqualTo("Custom prompt")
      items.node("[0].providerName").isEqualTo("organization-provider")
    }
  }

  @Test
  fun `won't show prompts to reviewer`() {
    this.userAccount = testData.projectReviewer.self
    performAuthGet(
      "/v2/projects/${testData.promptProject.self.id}/prompts",
    ).andIsForbidden
  }

  @Test
  fun `creates a prompt`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts",
      PromptDto("My prompt", "Hi LLM", "default"),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("My prompt")
      node("providerName").isEqualTo("default")
      node("template").isEqualTo("Hi LLM")
    }
  }

  @Test
  fun `denies reviewer prompt creation`() {
    this.userAccount = testData.projectReviewer.self
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts",
      PromptDto("My prompt", "Hi LLM", "default"),
    ).andIsForbidden
  }

  @Test
  fun `updates prompt`() {
    performAuthPut(
      "/v2/projects/${testData.promptProject.self.id}/prompts/${testData.customPrompt.self.id}",
      PromptDto("Updated prompt", "Hi LLM", "default"),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("Updated prompt")
      node("providerName").isEqualTo("default")
      node("template").isEqualTo("Hi LLM")
    }
  }

  @Test
  fun `deletes prompt`() {
    performAuthDelete(
      "/v2/projects/${testData.promptProject.self.id}/prompts/${testData.customPrompt.self.id}",
    ).andIsOk
  }

  @Test
  fun `denies reviewer prompt deletion`() {
    this.userAccount = testData.projectReviewer.self
    performAuthDelete(
      "/v2/projects/${testData.promptProject.self.id}/prompts/${testData.customPrompt.self.id}",
    ).andIsForbidden
  }

  @Test
  fun `runs prompt`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template =
          """
          Hi LLM
          key={{key.name}}
          translation={{source.translation}}
          """.trimIndent(),
        keyId = testData.keys.first().self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("Hi LLM")
      node("prompt").isString.contains("key=Key 1")
      node("prompt").isString.contains("translation=English translation 1")
      node("result").isString.contains("response from: default")
      node("parsedJson.output").isEqualTo("response from: default")
      node("parsedJson.contextDescription").isEqualTo("context description from: default")
      node("price").isNotEqualTo(0)
      node("usage.inputTokens").isEqualTo(42)
      node("usage.outputTokens").isEqualTo(42)
      node("usage.cachedTokens").isEqualTo(21)
    }
  }
}
