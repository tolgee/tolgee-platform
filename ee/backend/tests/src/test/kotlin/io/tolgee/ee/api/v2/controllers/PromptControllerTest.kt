package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.PromptTestData
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.LlmProviderType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PromptControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private lateinit var testData: PromptTestData

  @BeforeEach
  fun setup() {
    testData = PromptTestData()
    testData.addKeyWithoutTranslations()
    testDataService.saveTestData(testData.root)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          type = LlmProviderType.OPENAI,
          tokenPriceInCreditsInput = 2.0,
          tokenPriceInCreditsOutput = 2.0,
          apiUrl = "http://test.com",
        ),
      )
    internalProperties.fakeLlmProviders = true
    this.userAccount = testData.projectEditor.self
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_PROMPT_CUSTOMIZATION)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `returns default prompt`() {
    performAuthGet(
      "/v2/projects/${testData.promptProject.self.id}/prompts/default",
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("")
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
      PromptDto("My prompt", "default", "Hi LLM"),
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
      PromptDto("My prompt", "default", "Hi LLM"),
    ).andIsForbidden
  }

  @Test
  fun `updates prompt`() {
    performAuthPut(
      "/v2/projects/${testData.promptProject.self.id}/prompts/${testData.customPrompt.self.id}",
      PromptDto("Updated prompt", "default", "Hi LLM"),
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
  fun `includes character limit hint in prompt`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template =
          """
          {{fragment.charLimit}}
          """.trimIndent(),
        keyId =
          testData.keys
            .first()
            .self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("MUST NOT exceed 42 characters")
      node("prompt").isString.contains("Keep the translation concise")
    }
  }

  @Test
  fun `omits character limit hint when not set`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template =
          """
          {{fragment.charLimit}}
          """.trimIndent(),
        keyId =
          testData.keys[1]
            .self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.doesNotContain("MUST NOT exceed")
      node("prompt").isString.doesNotContain("character")
    }
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
        keyId =
          testData.keys
            .first()
            .self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
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
      node("usage.outputTokens").isEqualTo(21)
      node("usage.cachedTokens").isEqualTo(1)
    }
  }

  @Test
  fun `escapeJson helper escapes value for json embedding`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template =
          """{"description": "{{escapeJson key.description}}", "missing": "{{escapeJson key.nonexistent}}"}""",
        keyId = testData.keys[3].self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt")
        .isString
        .contains("""{"description": "Has \"quotes\" and\nnewline \\ backslash", "missing": ""}""")
    }
  }

  @Test
  fun `escapeJson does not double-escape an already escaped translation`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template = "bare={{source.translation}} wrapped={{escapeJson source.translation}}",
        keyId = testData.keys[3].self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("""bare=Says \"hi\"""")
      node("prompt").isString.contains("""wrapped=Says \"hi\"""")
      node("prompt").isString.doesNotContain("""\\\"""")
    }
  }

  @Test
  fun `keeps translation truthiness and empty rendering`() {
    val template = "[{{source.translation}}]{{#if source.translation}}yes{{else}}no{{/if}}"

    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template = template,
        keyId = testData.keyWithoutTranslations.self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("[]no")
    }

    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template = template,
        keyId = testData.keys[3].self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("""[Says \"hi\"]yes""")
    }
  }

  @Test
  fun `renders a plain string variable without html escaping`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template = "{{key.description}}",
        keyId = testData.keys[3].self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("""Has "quotes"""")
      node("prompt").isString.doesNotContain("&quot;")
    }
  }

  @Test
  fun `escapeJson renders empty for the variable context instead of dumping it`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template = "bare=[{{escapeJson}}] object=[{{escapeJson source}}]",
        keyId = testData.keys[3].self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("bare=[] object=[]")
    }
  }

  @Test
  fun `default translationInfo fragment escapes the translation`() {
    performAuthPost(
      "/v2/projects/${testData.promptProject.self.id}/prompts/run",
      PromptRunDto(
        template = "{{fragment.translationInfo}}",
        keyId = testData.keys[3].self.id,
        targetLanguageId = testData.czech.self.id,
        provider = "default",
        basicPromptOptions = null,
      ),
    ).andIsOk.andAssertThatJson {
      node("prompt").isString.contains("""Translate "Says \"hi\"" from English to Czech.""")
    }
  }
}
