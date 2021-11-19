package io.tolgee.controllers

import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.enums.ApiScope
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.UserApiAppAction
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@Deprecated("This is too complicated")
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

  protected fun createBaseWithApiKey(vararg scopes: ApiScope): ApiKeyDTO {
    var scopesSet = scopes.toSet()
    if (scopesSet.isEmpty()) {
      scopesSet = ApiScope.values().toSet()
    }
    val base = dbPopulator.createBase(generateUniqueString())
    return ApiKeyDTO.fromEntity(apiKeyService.create(base.permissions.first().user!!, scopesSet, base))
  }
}
