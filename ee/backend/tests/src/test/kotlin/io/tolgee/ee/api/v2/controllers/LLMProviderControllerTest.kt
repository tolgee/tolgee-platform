package io.tolgee.ee.api.v2.controllers

import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.dtos.request.llmProvider.LLMProviderRequest
import io.tolgee.ee.development.PromptTestData
import io.tolgee.fixtures.*
import io.tolgee.model.enums.LLMProviderType
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LLMProviderControllerTest : AuthorizedControllerTest() {
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
      node("items").isArray.hasSize(2)
      node("items[0].name").isEqualTo("organization-provider")
      node("items[1].name").isEqualTo("default")
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
      node("items").isArray.hasSize(1)
      node("items[0].name").isEqualTo("default")
      node("items[0].id").isEqualTo("-1")
    }
  }

  @Test
  fun `returns organization llm providers`() {
    performAuthGet(
      "/v2/organizations/${testData.organization.self.id}/llm-providers",
    ).andIsOk.andAssertThatJson {
      node("items").isArray.hasSize(1)
      node("items[0].name").isEqualTo("organization-provider")
    }
  }

  @Test
  fun `adds llm provider`() {
    performAuthPost(
      "/v2/organizations/${testData.organization.self.id}/llm-providers",
      LLMProviderRequest(name = "custom-provider", type = LLMProviderType.OPENAI, apiUrl = "mock"),
    ).andIsOk
  }

  @Test
  fun `denies llm provider creation to org member`() {
    this.userAccount = testData.organizationMember.self
    performAuthPost(
      "/v2/organizations/${testData.organization.self.id}/llm-providers",
      LLMProviderRequest(name = "custom-provider", type = LLMProviderType.OPENAI, apiUrl = "mock"),
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
      LLMProviderRequest(name = "updated-provider", type = LLMProviderType.OLLAMA, apiUrl = "different-url"),
    ).andIsOk.andAssertThatJson {
      node("name").isEqualTo("updated-provider")
      node("type").isEqualTo("OLLAMA")
      node("apiUrl").isEqualTo("different-url")
    }
  }

  @Test
  fun `denies member to update provider`() {
    this.userAccount = testData.organizationMember.self
    performAuthPut(
      "/v2/organizations/${testData.organization.self.id}/llm-providers/${testData.llmProvider.self.id}",
      LLMProviderRequest(name = "updated-provider", type = LLMProviderType.OLLAMA, apiUrl = "different-url"),
    ).andIsForbidden
  }
}
