package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.ApiKeysTestData
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.dtos.request.apiKey.CreateApiKeyDto
import io.tolgee.dtos.request.apiKey.V2EditApiKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.ApiScope
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class V2ApiKeyControllerTest : AuthorizedControllerTest() {

  lateinit var testData: ApiKeysTestData

  @BeforeEach
  fun createData() {
    testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `creates API key`() {
    performAuthPost(
      "/v2/api-keys",
      CreateApiKeyDto().apply {
        projectId = testData.projectBuilder.self.id
        scopes = setOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.SCREENSHOTS_UPLOAD)
      }
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("key").isString.hasSizeGreaterThan(10)
      node("username").isEqualTo("test_username")
      node("userFullName").isEqualTo("")
      node("projectId").isNumber.isGreaterThan(BigDecimal(0))
      node("id").isValidId
      node("projectName").isEqualTo("test_project")
      node("scopes").isArray.isEqualTo("""[ "screenshots.upload", "translations.view" ]""")
    }
  }

  @Test
  fun `doesn't create when user has no project permission`() {
    userAccount = testData.frantisekDobrota
    performAuthPost(
      "/v2/api-keys",
      CreateApiKeyDto().apply {
        projectId = testData.projectBuilder.self.id
        scopes = setOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.SCREENSHOTS_UPLOAD)
      }
    ).andIsForbidden
  }

  @Test
  fun `doesn't create when user's permissions doesn't satisfy the scope requirements`() {
    this.userAccount = testData.frantisekDobrota
    performAuthPost(
      "/v2/api-keys",
      CreateApiKeyDto().apply {
        projectId = testData.projectBuilder.self.id
        scopes = setOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.SCREENSHOTS_UPLOAD, ApiScope.KEYS_EDIT)
      }
    ).andIsForbidden
  }

  @Test
  fun `validates creation body`() {
    performAuthPost(
      "/v2/api-keys",
      CreateApiKeyDto().apply {
        projectId = testData.frantasProject.id
        scopes = setOf()
      }
    ).andIsBadRequest
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to testData.frantasProject.id
      )
    ).andIsBadRequest
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to null,
        "scopes" to listOf("translations.edit")
      )
    ).andIsBadRequest
  }

  @Test
  fun `returns correct keys by user`() {
    userAccount = testData.frantisekDobrota
    performAuthGet("/v2/api-keys?page=2").andPrettyPrint.andIsOk.andAssertThatJson {
      node("page.totalElements").isNumber.isGreaterThan(BigDecimal(98)).isLessThan(BigDecimal(150))
      node("_embedded.apiKeys") {
        isArray.hasSize(20)
        node("[0]") {
          node("key").isEqualTo("test_api_key_franta_39")
          node("username").isEqualTo("franta")
          node("userFullName").isEqualTo("Franta Dobrota")
          node("projectId").isNumber.isGreaterThan(BigDecimal(0))
          node("id").isValidId
          node("scopes").isArray.isNotEmpty
        }
      }
    }
  }

  @Test
  fun `returns correct keys by user and filters project`() {
    userAccount = testData.frantisekDobrota
    performAuthGet("/v2/api-keys?filterProjectId=${testData.projectBuilder.self.id}")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("page.totalElements").isNumber.isEqualTo("1")
        node("_embedded.apiKeys") {
          isArray.hasSize(1)
          node("[0]") {
            node("key").isEqualTo("test_api_key_1")
            node("username").isEqualTo("franta")
            node("userFullName").isEqualTo("Franta Dobrota")
            node("projectId").isNumber.isGreaterThan(BigDecimal(0))
            node("id").isValidId
            node("scopes").isArray.isNotEmpty
          }
        }
      }
  }

  @Test
  fun `returns correct keys by project`() {
    performAuthGet("/v2/projects/${testData.frantasProject.id}/api-keys")
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("_embedded.apiKeys") {
          isArray.hasSize(20)
        }
        node("page.totalElements").isNumber.isGreaterThan(BigDecimal(98))
      }
  }

  @Test
  fun `updates existing key`() {
    userAccount = testData.frantisekDobrota
    performAuthPut("/v2/api-keys/${testData.frantasKey.id}", V2EditApiKeyDto(setOf(ApiScope.TRANSLATIONS_EDIT)))
      .andPrettyPrint.andIsOk.andAssertThatJson {
        node("scopes").isEqualTo("""["translations.edit"]""")
      }
  }

  @Test
  fun `doesn't update existing key when not satisfies permissions`() {
    userAccount = testData.frantisekDobrota
    performAuthPut("/v2/api-keys/${testData.frantasKey.id}", V2EditApiKeyDto(setOf(ApiScope.KEYS_EDIT)))
      .andIsForbidden
  }

  @Test
  fun `doesn't update existing key for different user`() {
    userAccount = testData.frantisekDobrota
    performAuthPut("/v2/api-keys/${testData.usersKey.id}", V2EditApiKeyDto(setOf(ApiScope.KEYS_EDIT)))
      .andIsForbidden
  }

  @Test
  fun `returns correct with get (keyId)`() {
    performAuthGet("/v2/api-keys/${testData.usersKey.id}").andPrettyPrint.andAssertThatJson {
      node("id").isValidId
      node("key").isEqualTo("test_api_key_2")
    }
  }

  @Test
  fun `doesn't return without permissions (keyId)`() {
    userAccount = testData.frantisekDobrota
    performAuthGet("/v2/api-keys/${testData.usersKey.id}").andIsForbidden
  }

  @Test
  fun `returns correct with get (current)`() {
    performAuthGet("/v2/api-keys/current?ak=${testData.usersKey.key}").andPrettyPrint.andAssertThatJson {
      node("id").isValidId
      node("key").isEqualTo("test_api_key_2")
    }
  }

  @Test
  fun `deletes key`() {
    performAuthDelete("/v2/api-keys/${testData.usersKey.id}", null).andIsOk
    assertThat(apiKeyService.getApiKey(testData.usersKey.id)).isEmpty
  }

  @Test
  fun `doesn't delete when not owning or manage`() {
    userAccount = testData.frantisekDobrota
    performAuthDelete("/v2/api-keys/${testData.usersKey.id}", null).andIsForbidden
  }

  @Test
  fun `deletes when manage`() {
    performAuthDelete("/v2/api-keys/${testData.frantasKey.id}", null).andIsOk
  }

  @Test
  fun `returns correct permitted languages for current`() {
    val testData = LanguagePermissionsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.bothLangsExplicitUser
    performAuthGet("/v2/api-keys/current?ak=${testData.bothLangsExplicitUserApiKey.key}")
      .andPrettyPrint.andAssertThatJson {
        node("id").isValidId
        node("permittedLanguageIds")
          .isArray
          .hasSize(2)
          .contains(testData.germanLanguage.id)
          .contains(testData.englishLanguage.id)
      }
  }
}
