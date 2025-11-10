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
import io.tolgee.model.enums.Scope
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import java.math.BigDecimal
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyControllerTest : AuthorizedControllerTest() {
  lateinit var testData: ApiKeysTestData

  @BeforeEach
  fun createData() {
    testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  fun `creates API key (without description and expiration)`() {
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to testData.projectBuilder.self.id,
        "scopes" to setOf(Scope.TRANSLATIONS_VIEW.value, Scope.SCREENSHOTS_UPLOAD.value),
      ),
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
  fun `creates API key`() {
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to testData.projectBuilder.self.id,
        "scopes" to setOf(Scope.TRANSLATIONS_VIEW.value, Scope.SCREENSHOTS_UPLOAD.value),
      ),
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
        scopes = setOf(Scope.TRANSLATIONS_VIEW, Scope.SCREENSHOTS_UPLOAD)
      },
    ).andIsForbidden
  }

  @Test
  fun `doesn't create when user's permissions doesn't satisfy the scope requirements`() {
    this.userAccount = testData.frantisekDobrota
    performAuthPost(
      "/v2/api-keys",
      CreateApiKeyDto().apply {
        projectId = testData.projectBuilder.self.id
        scopes = setOf(Scope.TRANSLATIONS_VIEW, Scope.SCREENSHOTS_UPLOAD, Scope.KEYS_EDIT)
      },
    ).andIsForbidden
  }

  @Test
  fun `validates creation body`() {
    performAuthPost(
      "/v2/api-keys",
      CreateApiKeyDto().apply {
        projectId = testData.frantasProject.id
        scopes = setOf()
      },
    ).andIsBadRequest
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to testData.frantasProject.id,
      ),
    ).andIsBadRequest
    performAuthPost(
      "/v2/api-keys",
      mapOf(
        "projectId" to null,
        "scopes" to listOf("translations.edit"),
      ),
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
          node("key").isAbsent()
          node("description").isEqualTo("test_......ta_39")
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
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("page.totalElements").isNumber.isEqualTo("1")
        node("_embedded.apiKeys") {
          isArray.hasSize(1)
          node("[0]") {
            node("key").isAbsent()
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
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("_embedded.apiKeys") {
          isArray.hasSize(20)
        }
        node("page.totalElements").isNumber.isGreaterThan(BigDecimal(98))
      }
  }

  @Test
  fun `updates existing key`() {
    userAccount = testData.frantisekDobrota
    performAuthPut("/v2/api-keys/${testData.frantasKey.id}", V2EditApiKeyDto(setOf(Scope.TRANSLATIONS_EDIT)))
      .andPrettyPrint.andIsOk
      .andAssertThatJson {
        node("scopes").isEqualTo("""["translations.edit"]""")
      }
  }

  @Test
  fun `doesn't update existing key when not satisfies permissions`() {
    userAccount = testData.frantisekDobrota
    performAuthPut("/v2/api-keys/${testData.frantasKey.id}", V2EditApiKeyDto(setOf(Scope.KEYS_EDIT)))
      .andIsForbidden
  }

  @Test
  fun `doesn't update existing key for different user`() {
    userAccount = testData.frantisekDobrota
    performAuthPut("/v2/api-keys/${testData.usersKey.id}", V2EditApiKeyDto(setOf(Scope.KEYS_EDIT)))
      .andIsForbidden
  }

  @Test
  fun `returns correct with get (keyId)`() {
    performAuthGet("/v2/api-keys/${testData.usersKey.id}").andPrettyPrint.andAssertThatJson {
      node("id").isValidId
      node("key").isAbsent()
    }
  }

  @Test
  fun `doesn't return without permissions (keyId)`() {
    userAccount = testData.frantisekDobrota
    performAuthGet("/v2/api-keys/${testData.usersKey.id}").andIsForbidden
  }

  @Test
  fun `returns correct with get (current)`() {
    val headers = HttpHeaders()
    headers["x-api-key"] = testData.usersKey.key!!
    performGet("/v2/api-keys/current", headers).andPrettyPrint.andAssertThatJson {
      node("id").isValidId
    }
  }

  @Test
  fun `returns correct current permissions PAK`() {
    val headers = HttpHeaders()
    headers["x-api-key"] = testData.usersKey.key!!
    performGet("/v2/api-keys/current-permissions", headers).andPrettyPrint.andAssertThatJson {
      node("projectId").isNotNull
      node("scopes").isArray.isNotEmpty
      node("translateLanguageIds").isNull()
      node("viewLanguageIds").isNull()
      node("stateChangeLanguageIds").isNull()
      node("project") {
        node("id").isNumber.isGreaterThan(0.toBigDecimal())
      }
    }
  }

  @Test
  fun `returns correct current permissions PAT`() {
    val headers = HttpHeaders()
    headers["x-api-key"] = "tgpat_${testData.frantasPat.token!!}"
    performGet("/v2/api-keys/current-permissions?projectId=${testData.frantasProject.id}", headers)
      .andIsOk
      .andAssertThatJson {
        node("projectId").isNotNull
        node("type").isEqualTo("MANAGE")
        node("scopes").isArray.isNotEmpty
        node("translateLanguageIds").isNull()
        node("viewLanguageIds").isNull()
        node("stateChangeLanguageIds").isNull()
      }
  }

  @Test
  fun `returns 400 when trying to get current key with Bearer auth`() {
    loginAsUser(testData.frantisekDobrota)
    performAuthGet("/v2/api-keys/current").andIsBadRequest
  }

  @Test
  fun `returns 403 when trying to get current key with PAT auth`() {
    val headers = HttpHeaders()
    headers["x-api-key"] = "tgpat_${testData.frantasPat.token!!}"
    performGet("/v2/api-keys/current", headers).andIsForbidden
  }

  @Test
  fun `deletes key`() {
    performAuthDelete("/v2/api-keys/${testData.usersKey.id}", null).andIsOk
    assertThat(apiKeyService.findOptional(testData.usersKey.id)).isEmpty
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
    userAccount = testData.translateAllExplicitUser
    performGet("/v2/api-keys/current?ak=${testData.bothLangsExplicitUserApiKey.key}")
      .andPrettyPrint
      .andAssertThatJson {
        node("id").isValidId
        node("permittedLanguageIds")
          .isArray
          .hasSize(2)
          .contains(testData.germanLanguage.id)
          .contains(testData.englishLanguage.id)
      }
  }

  @Test
  fun `regenerate works on expired key`() {
    val oldKeyHash = testData.expiredKey.keyHash

    val expiresAt = Date().time + 10000
    performAuthPut(
      "/v2/api-keys/${testData.expiredKey.id}/regenerate",
      mapOf(
        "expiresAt" to expiresAt,
      ),
    ).andIsOk.andAssertThatJson {
      node("key").isString.startsWith("tgpak_").hasSizeGreaterThan(20)
      node("expiresAt").isEqualTo(expiresAt)
    }

    val key = apiKeyService.get(testData.expiredKey.id)
    key.key.assert.isNull()
    key.keyHash.assert.isNotEqualTo(oldKeyHash)
  }

  @Test
  fun `regenerate works (never expiring key)`() {
    val oldKeyHash = testData.usersKey.keyHash
    testData.usersKey.expiresAt.assert
      .isNull()

    performAuthPut(
      "/v2/api-keys/${testData.usersKey.id}/regenerate",
      mapOf(
        "expiresAt" to null,
      ),
    ).andIsOk.andAssertThatJson {
      node("key").isString.startsWith("tgpak_").hasSizeGreaterThan(20)
    }

    val key = apiKeyService.get(testData.usersKey.id)
    key.key.assert.isNull()
    key.keyHash.assert.isNotEqualTo(oldKeyHash)
  }
}
