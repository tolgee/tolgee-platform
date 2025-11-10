package io.tolgee.ee.api.v2.controllers

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.development.testDataBuilder.data.PromptTestData
import io.tolgee.dtos.request.llmProvider.LlmProviderRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LlmProviderControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: PromptTestData

  @BeforeEach
  fun setup() {
    testData = PromptTestData()
    testDataService.saveTestData(testData.root)
    llmProperties.enabled = true
    llmProperties.providers =
      mutableListOf(
        LlmProperties.LlmProvider(
          type = LlmProviderType.OPENAI,
          apiUrl = "http://test.com",
        ),
      )
    this.userAccount = testData.organizationOwner.self
  }

  @Test
  fun `returns all available llm providers`() {
    this.userAccount = testData.projectReviewer.self
    performAuthGet(
      "/v2/organizations/${testData.organization.self.id}/llm-providers/all-available",
    ).andIsOk.andAssertThatJson {
      node("_embedded.providers").isArray.hasSize(2)
      node("_embedded.providers[0].name").isEqualTo("organization-provider")
      node("_embedded.providers[1].name").isEqualTo("default")
    }
  }

  @Test
  fun `prevents viewing other organization providers`() {
    this.userAccount = testData.projectReviewer.self
    performAuthGet(
      "/v2/organizations/${testData.unrelatedOrganization.self.id}/llm-providers/all-available",
    ).andIsNotFound
  }

  @Test
  fun `returns server llm providers`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.self.id}/llm-providers/server-providers",
    ).andIsOk.andAssertThatJson {
      node("_embedded.providers").isArray.hasSize(1)
      node("_embedded.providers[0].name").isEqualTo("default")
      node("_embedded.providers[0].type").isEqualTo("OPENAI")
    }
  }

  @Test
  fun `returns organization llm providers`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.self.id}/llm-providers",
    ).andIsOk.andAssertThatJson {
      node("_embedded.providers").isArray.hasSize(1)
      node("_embedded.providers[0].name").isEqualTo("organization-provider")
    }
  }

  @Test
  fun `adds llm provider`() {
    performAuthPost(
      "/v2/organizations/${testData.organization.self.id}/llm-providers",
      LlmProviderRequest(name = "custom-provider", type = LlmProviderType.OPENAI, apiUrl = "mock"),
    ).andIsOk
  }

  @Test
  fun `denies llm provider creation to org member`() {
    this.userAccount = testData.organizationMember.self
    performAuthPost(
      "/v2/organizations/${testData.organization.self.id}/llm-providers",
      LlmProviderRequest(name = "custom-provider", type = LlmProviderType.OPENAI, apiUrl = "mock"),
    ).andIsForbidden.andAssertThatJson {
      node("code").isEqualTo("operation_not_permitted")
    }
  }

  @Test
  fun `deletes llm provider`() {
    performAuthDelete(
      "/v2/organizations/${testData.organization.self.id}/llm-providers/${testData.llmProvider.self.id}",
    ).andIsOk
  }

  @Test
  fun `member is denied llm deletion`() {
    this.userAccount = testData.organizationMember.self
    performAuthDelete(
      "/v2/organizations/${testData.organization.self.id}/llm-providers/${testData.llmProvider.self.id}",
    ).andIsForbidden
  }

  @Test
  fun `updates llm provider`() {
    performAuthPut(
      "/v2/organizations/${testData.organization.self.id}/llm-providers/${testData.llmProvider.self.id}",
      LlmProviderRequest(name = "updated-provider", type = LlmProviderType.OPENAI_AZURE, apiUrl = "different-url"),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("updated-provider")
      node("type").isEqualTo("OPENAI_AZURE")
      node("apiUrl").isEqualTo("different-url")
    }
  }

  @Test
  fun `denies member to update provider`() {
    this.userAccount = testData.organizationMember.self
    performAuthPut(
      "/v2/organizations/${testData.organization.self.id}/llm-providers/${testData.llmProvider.self.id}",
      LlmProviderRequest(name = "updated-provider", type = LlmProviderType.OPENAI_AZURE, apiUrl = "different-url"),
    ).andIsForbidden
  }
}
