package io.polygloat.security

import io.polygloat.Assertions.Assertions
import io.polygloat.Assertions.UserApiAppAction
import io.polygloat.constants.ApiScope
import io.polygloat.controllers.AbstractUserAppApiTest
import io.polygloat.dtos.request.SetTranslationsDTO
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
        val apiKey = apiKeyService.createApiKey(base.createdBy, setOf(*ApiScope.values()), base)
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
        val apiKey = apiKeyService.createApiKey(base.createdBy, setOf(*ApiScope.values()), base)
        performAction(UserApiAppAction(
                apiKey = apiKey.key,
                url = "/api/repositories",
                expectedStatus = HttpStatus.FORBIDDEN
        ))
        mvc.perform(MockMvcRequestBuilders.get("/api/repositories"))
                .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
    }

    @Test
    fun accessWithApiKey_listPermissions() {
        var apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_VIEW)
        performAction(UserApiAppAction(apiKey = apiKey.key, url = "/uaa/en", expectedStatus = HttpStatus.OK))
        apiKey = createBaseWithApiKey(ApiScope.SOURCES_EDIT)
        performAction(UserApiAppAction(apiKey = apiKey.key, url = "/uaa/en", expectedStatus = HttpStatus.FORBIDDEN))
    }

    @Test
    fun accessWithApiKey_editPermissions() {
        var apiKey = createBaseWithApiKey(ApiScope.SOURCES_EDIT)
        val translations = SetTranslationsDTO.builder()
                .key("aaaa")
                .translations(mapOf<String, String>(Pair("aaa", "aaa"))).build() //just a fake to pass validation
        performAction(UserApiAppAction(
                method = HttpMethod.POST,
                body = translations,
                apiKey = apiKey.key,
                url = "/uaa",
                expectedStatus = HttpStatus.FORBIDDEN
        ))
        apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_EDIT)
        performAction(UserApiAppAction(
                method = HttpMethod.POST,
                body = translations,
                apiKey = apiKey.key,
                url = "/uaa",
                expectedStatus = HttpStatus.NOT_FOUND))
    }

    @Test
    fun accessWithApiKey_getLanguages() {
        var apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_VIEW)
        performAction(UserApiAppAction(
                method = HttpMethod.GET,
                apiKey = apiKey.key,
                url = "/uaa/languages",
                expectedStatus = HttpStatus.FORBIDDEN))

        apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_EDIT)

        performAction(UserApiAppAction(
                method = HttpMethod.GET,
                apiKey = apiKey.key,
                url = "/uaa/languages",
                expectedStatus = HttpStatus.OK))
    }
}