package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.ee.development.PromptTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.LLMProviderType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiPlaygroundResultControllerTest : ProjectAuthControllerTest("/v2/projects/") {
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
    this.userAccount = testData.serverAdmin.self
  }

  @Test
  fun `doesn't block project deletion`() {
    performAuthDelete(
      "/v2/projects/${testData.promptProject.self.id}",
    ).andIsOk
  }

  @Test
  fun `doesn't block user deletion`() {
    performAuthDelete(
      "/v2/administration/users/${testData.organizationMember.self.id}",
    ).andIsOk
  }

  @Test
  fun `doesn't block key deletion`() {
    performAuthDelete(
      "/v2/projects/${testData.promptProject.self.id}/keys/${testData.keys[0].self.id}",
    ).andIsOk
  }

  @Test
  fun `doesn't block language deletion`() {
    performAuthDelete(
      "/v2/projects/${testData.promptProject.self.id}/languages/${testData.czech.self.id}",
    ).andIsOk
  }
}
