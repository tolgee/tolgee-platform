package io.tolgee.security

import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.development.testDataBuilder.data.ApiKeysTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@AutoConfigureMockMvc
class ProjectApiKeyAuthenticationTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtService: JwtService

  @Test
  fun after() {
    currentDateProvider.forcedDate = null
  }

  @Test
  fun accessWithApiKey_failure() {
    mvc.perform(MockMvcRequestBuilders.get("/v2/projects/translations")).andIsForbidden
  }

  @Test
  fun `access with legacy key works`() {
    val base = dbPopulator.createBase()
    val apiKey = apiKeyService.create(base.userAccount, setOf(*Scope.values()), base.project)
    mvc.perform(MockMvcRequestBuilders.get("/v2/projects/translations?ak=" + apiKey.key)).andIsOk
  }

  @Test
  fun accessWithApiKey_failure_wrong_key() {
    mvc
      .perform(MockMvcRequestBuilders.get("/v2/projects/translations?ak=wrong_api_key"))
      .andExpect(MockMvcResultMatchers.status().isUnauthorized)
      .andReturn()
  }

  @Test
  fun accessWithApiKey_failure_api_path() {
    val base = dbPopulator.createBase()
    val apiKey = apiKeyService.create(base.userAccount, setOf(*Scope.entries.toTypedArray()), base.project)
    mvc.perform(MockMvcRequestBuilders.get("/v2/projects?ak=${apiKey.key}")).andIsForbidden
  }

  @Test
  fun `works with tgpak_ prefix`() {
    val testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)

    currentDateProvider.forcedDate = currentDateProvider.date

    performGet(
      "/v2/api-keys/current",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_" + testData.frantasKey.encodedKey)
      },
    ).andIsOk.andAssertThatJson {
      node("description").isNotNull
    }

    waitForNotThrowing(throwableClass = AssertionError::class, timeout = 5000) {
      executeInNewTransaction {
        apiKeyService
          .get(testData.frantasKey.id)
          .lastUsedAt
          ?.time
          .assert
          .isEqualTo(currentDateProvider.forcedDate!!.time)
      }
    }
  }

  @Test
  fun `expired key is unauthorized`() {
    val testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)

    performGet(
      "/v2/api-keys/current",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_" + testData.expiredKey.encodedKey)
      },
    ).andIsUnauthorized
  }

  @Test
  fun `access to different project is forbidden`() {
    val testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)

    performGet(
      "/v2/projects/${testData.frantasProject.id}",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_" + testData.usersKey.encodedKey)
      },
    ).andIsForbidden
  }

  @Test
  fun `access to authorized project is OK`() {
    val testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)

    performGet(
      "/v2/projects/${testData.projectBuilder.self.id}",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_" + testData.usersKey.encodedKey)
      },
    ).andIsOk
  }

  @Test
  fun `malformed API key is unauthorized`() {
    val testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)

    performGet(
      "/v2/projects/${testData.frantasProject.id}",
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_---aaajsjs")
      },
    ).andIsUnauthorized
  }

  @Test
  fun `permissions get correctly revoked when the user no longer have them`() {
    val testData = ApiKeysTestData()
    testDataService.saveTestData(testData.root)

    performPut(
      "/v2/projects/${testData.frantasProject.id}/translations/${testData.frantasTranslation.id}/set-state/REVIEWED",
      null,
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_" + testData.usersKeyFrantasProject.encodedKey)
      },
    ).andIsOk

    // Revoke user permissions
    val tokenFrantisek = jwtService.emitToken(testData.frantisekDobrota.id, isSuper = true)
    performPut(
      "/v2/projects/${testData.frantasProject.id}/users/${testData.user.id}/set-permissions/VIEW",
      null,
      HttpHeaders().apply {
        add("Authorization", "Bearer $tokenFrantisek")
      },
    ).andIsOk

    // Test if PAK is no longer able to set state
    performPut(
      "/v2/projects/${testData.frantasProject.id}/translations/${testData.frantasTranslation.id}/set-state/TRANSLATED",
      null,
      HttpHeaders().apply {
        add(API_KEY_HEADER_NAME, "tgpak_" + testData.usersKeyFrantasProject.encodedKey)
      },
    ).andIsForbidden
  }
}
