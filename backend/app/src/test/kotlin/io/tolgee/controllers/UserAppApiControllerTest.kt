package io.tolgee.controllers

import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.model.enums.ApiScope
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@AutoConfigureMockMvc
class UserAppApiControllerTest : AbstractUserAppApiTest() {
  @Test
  fun getScopes() {
    val base = dbPopulator.createBase(generateUniqueString())
    val apiKey = apiKeyService.create(base.permissions.first().user!!, setOf(*ApiScope.values()), base)
    mvc.perform(MockMvcRequestBuilders.get("/uaa/scopes?ak=" + apiKey.key))
      .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
  }

  @Test
  fun getLanguages() {
    val base = dbPopulator.createBase(generateUniqueString())
    val apiKey = apiKeyService.create(base.permissions.first().user!!, setOf(*ApiScope.values()), base)
    val languages = mvc.perform(MockMvcRequestBuilders.get("/uaa/languages?ak=" + apiKey.key))
      .andExpect(MockMvcResultMatchers.status().isOk).andReturn().mapResponseTo<Set<String>>()
  }
}
