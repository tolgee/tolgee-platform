package io.tolgee.controllers

import io.tolgee.dtos.response.ApiKeyDTO
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.enums.Scope
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assertions.PakAction
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@Deprecated("This is too complicated")
abstract class AbstractApiKeyTest : AbstractControllerTest() {
  fun performAction(action: PakAction): MvcResult {
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

  protected fun createBaseWithApiKey(vararg scopes: Scope): ApiKeyDTO {
    var scopesSet = scopes.toSet()
    if (scopesSet.isEmpty()) {
      scopesSet = Scope.values().toSet()
    }
    val base = dbPopulator.createBase(generateUniqueString())
    return io.tolgee.dtos.response.ApiKeyDTO.fromEntity(apiKeyService.create(base.userAccount, scopesSet, base.project))
  }
}
