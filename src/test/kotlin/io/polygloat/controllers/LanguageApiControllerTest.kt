package io.polygloat.controllers

import io.polygloat.Assertions.Assertions.assertThat
import io.polygloat.Assertions.UserApiAppAction
import io.polygloat.constants.ApiScope
import io.polygloat.dtos.request.LanguageDTO
import io.polygloat.exceptions.NotFoundException
import io.polygloat.helpers.JsonHelper
import org.assertj.core.api.Assertions
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class LanguageApiControllerTest : AbstractUserAppApiTest(), ITest {

    @Test
    fun findAllLanguages() {
        val repository = dbPopulator.createBase(generateUniqueString(), "ben")
        val apiKey = apiKeyService.createApiKey(repository.createdBy, setOf(*ApiScope.values()), repository)
        val contentAsString = performAction(UserApiAppAction(
                method = HttpMethod.GET,
                apiKey = apiKey.key,
                expectedStatus = HttpStatus.OK,
                url = "/api/languages"
        )).response.contentAsString
        assertThat(decodeJson<Set<*>>(contentAsString, Set::class.java)).hasSize(2)
    }
}