package io.tolgee.security

import io.tolgee.controllers.AbstractUserAppApiTest
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.enums.ApiScope
import io.tolgee.testing.assertions.Assertions
import io.tolgee.testing.assertions.UserApiAppAction
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@AutoConfigureMockMvc
class ApiKeyAuthenticationTest : AbstractUserAppApiTest() {
  @Test
  fun accessWithApiKey_failure() {
    val mvcResult = mvc.perform(MockMvcRequestBuilders.get("/uaa/en"))
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
    Assertions.assertThat(mvcResult).error()
  }

  @Test
  fun accessWithApiKey_success() {
    val base = dbPopulator.createBase(generateUniqueString())
    val apiKey = apiKeyService.create(base.permissions.first().user!!, setOf(*ApiScope.values()), base)
    mvc.perform(MockMvcRequestBuilders.get("/uaa/en?ak=" + apiKey.key))
      .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
  }

  @Test
  fun accessWithApiKey_failure_wrong_key() {
    mvc.perform(MockMvcRequestBuilders.get("/uaa/en?ak=wrong_api_key"))
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
  }

  @Test
  fun accessWithApiKey_failure_api_path() {
    val base = dbPopulator.createBase(generateUniqueString())
    val apiKey = apiKeyService.create(base.permissions.first().user!!, setOf(*ApiScope.values()), base)
    performAction(
      UserApiAppAction(
        apiKey = apiKey.key,
        url = "/api/projects",
        expectedStatus = HttpStatus.FORBIDDEN
      )
    )
    mvc.perform(MockMvcRequestBuilders.get("/api/projects"))
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
  }

  @Test
  fun accessWithApiKey_listPermissions() {
    var apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_VIEW)
    performAction(UserApiAppAction(apiKey = apiKey.key, url = "/uaa/en", expectedStatus = HttpStatus.OK))
    apiKey = createBaseWithApiKey(ApiScope.KEYS_EDIT)
    performAction(UserApiAppAction(apiKey = apiKey.key, url = "/uaa/en", expectedStatus = HttpStatus.FORBIDDEN))
  }

  @Test
  fun accessWithApiKey_editPermissions() {
    var apiKey = createBaseWithApiKey(ApiScope.KEYS_EDIT)
    val translations = SetTranslationsWithKeyDto(key = "aaaa", translations = mapOf(Pair("aaa", "aaa")))

    performAction(
      UserApiAppAction(
        method = HttpMethod.POST,
        body = translations,
        apiKey = apiKey.key,
        url = "/uaa",
        expectedStatus = HttpStatus.FORBIDDEN
      )
    )
    apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_EDIT)
    performAction(
      UserApiAppAction(
        method = HttpMethod.POST,
        body = translations,
        apiKey = apiKey.key,
        url = "/uaa",
        expectedStatus = HttpStatus.NOT_FOUND
      )
    )
  }

  @Test
  fun accessWithApiKey_getLanguages() {
    var apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_VIEW)
    performAction(
      UserApiAppAction(
        method = HttpMethod.GET,
        apiKey = apiKey.key,
        url = "/uaa/languages",
        expectedStatus = HttpStatus.FORBIDDEN
      )
    )

    apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_EDIT)

    performAction(
      UserApiAppAction(
        method = HttpMethod.GET,
        apiKey = apiKey.key,
        url = "/uaa/languages",
        expectedStatus = HttpStatus.OK
      )
    )
  }
}
