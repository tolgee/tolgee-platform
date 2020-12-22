package io.polygloat.controllers

import io.polygloat.assertions.UserApiAppAction
import io.polygloat.constants.ApiScope
import io.polygloat.dtos.response.ApiKeyDTO.ApiKeyDTO
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

abstract class AbstractUserAppApiTest : AbstractControllerTest() {
    fun performAction(action: UserApiAppAction): MvcResult {
        return try {
            var resultActions = mvc.perform(action.requestBuilder)
            if (action.expectedStatus != null) {
                resultActions = resultActions.andExpect(MockMvcResultMatchers.status().`is`(action.expectedStatus!!.value()))
            }
            resultActions.andReturn()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    protected fun createBaseWithApiKey(vararg scopes: ApiScope?): ApiKeyDTO {
        var scopesSet = setOf(*scopes)
        if (scopesSet.isEmpty()) {
            scopesSet = setOf(*ApiScope.values())
        }
        val base = dbPopulator.createBase(generateUniqueString())
        return apiKeyService.createApiKey(base.createdBy, scopesSet, base)
    }
}