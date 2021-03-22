package io.tolgee.controllers

import io.tolgee.constants.ApiScope
import io.tolgee.fixtures.parseResponseTo
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

@AutoConfigureMockMvc
class UserAppApiControllerTest : AbstractUserAppApiTest() {
    @Test
    fun getScopes() {
        val base = dbPopulator.createBase(generateUniqueString())
        val apiKey = apiKeyService.createApiKey(base.createdBy, setOf(*ApiScope.values()), base)
        mvc.perform(MockMvcRequestBuilders.get("/uaa/scopes?ak=" + apiKey.key))
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    }

    @Test
    fun getLanguages() {
        val base = dbPopulator.createBase(generateUniqueString())
        val apiKey = apiKeyService.createApiKey(base.createdBy, setOf(*ApiScope.values()), base)
        val languages = mvc.perform(MockMvcRequestBuilders.get("/uaa/languages?ak=" + apiKey.key))
                .andExpect(MockMvcResultMatchers.status().isOk).andReturn().parseResponseTo<Set<String>>()
    }
}