package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.PromptTestData
import io.tolgee.ee.data.AiPlaygroundResultRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiPlaygroundResultControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: PromptTestData

  @BeforeEach
  fun setup() {
    testData = PromptTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.promptProject.self }
    this.userAccount = testData.projectEditor.self
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `will return ai prompt results`() {
    performProjectAuthPost(
      "ai-playground-result",
      AiPlaygroundResultRequest(
        keys = testData.keys.map { it.self.id },
        languages = listOf(testData.czech.self.id),
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.results[0].translation").isString.contains("Llm test response")
    }
  }
}
