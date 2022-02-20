package io.tolgee.controllers

import com.fasterxml.jackson.databind.type.TypeFactory
import io.tolgee.dtos.request.apiKey.CreateApiKeyDto
import io.tolgee.dtos.request.apiKey.EditApiKeyDTO
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.model.Project
import io.tolgee.model.enums.ApiScope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*
import java.util.stream.Collectors

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyControllerTest : ProjectAuthControllerTest() {

  @Test
  fun create_success() {
    val apiKeyDTO = doCreate()
    val apiKey = apiKeyService.getApiKey(apiKeyDTO.key)
    Assertions.assertThat(apiKey).isPresent
    checkKey(apiKey.get().key)
  }

  private fun doCreate(username: String): ApiKeyDTO {
    return doCreate(dbPopulator.createBase(generateUniqueString(), username))
  }

  private fun doCreate(project: Project = dbPopulator.createBase(generateUniqueString())): ApiKeyDTO {
    val requestDto = CreateApiKeyDto(
      projectId = project.id,
      scopes = setOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.KEYS_EDIT)
    )
    val mvcResult = performAuthPost("/api/apiKeys", requestDto)
      .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    return mapResponse(mvcResult, ApiKeyDTO::class.java)
  }

  @Test
  fun create_failure_no_scopes() {
    val base = dbPopulator.createBase(generateUniqueString())
    val requestDto = CreateApiKeyDto(base.id, setOf())
    val mvcResult = performAuthPost("/api/apiKeys", requestDto)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    assertThat(mvcResult).error().isStandardValidation.onField("scopes").isEqualTo("must not be empty")
    assertThat(mvcResult).error().isStandardValidation.errorCount().isEqualTo(1)
  }

  @Test
  fun create_failure_no_project() {
    val requestDto = CreateApiKeyDto(scopes = setOf(ApiScope.TRANSLATIONS_VIEW), projectId = 0)
    val mvcResult = performAuthPost("/api/apiKeys", requestDto)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    assertThat(mvcResult).error().isStandardValidation
      .onField("projectId")
      .isEqualTo("must be greater than or equal to 1")
    assertThat(mvcResult).error().isStandardValidation.errorCount().isEqualTo(1)
  }

  @Test
  fun edit_success() {
    val apiKeyDTO = doCreate()
    val newScopes = setOf(ApiScope.TRANSLATIONS_EDIT)
    val editDto = EditApiKeyDTO(id = apiKeyDTO.id, scopes = newScopes)
    performAuthPost("/api/apiKeys/edit", editDto).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    val apiKey = apiKeyService.getApiKey(apiKeyDTO.id)
    Assertions.assertThat(apiKey).isPresent
    Assertions.assertThat(apiKey.get().scopesEnum).isEqualTo(newScopes)
  }

  @Test
  fun edit_failure_no_scopes() {
    val newScopes: Set<ApiScope> = setOf()
    val apiKeyDTO = doCreate()
    val editDto = EditApiKeyDTO(id = apiKeyDTO.id, scopes = newScopes)
    val mvcResult = performAuthPost("/api/apiKeys/edit", editDto)
      .andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
    assertThat(mvcResult).error().isStandardValidation.onField("scopes").isEqualTo("must not be empty")
  }

  @Test
  fun getAllByUser() {
    val project = dbPopulator.createBase(generateUniqueString(), "ben")
    loginAsUser("ben")
    val apiKey1 = apiKeyService.create(project.permissions.first().user!!, setOf(ApiScope.KEYS_EDIT), project)
    val project2 = dbPopulator.createBase(generateUniqueString(), "ben")
    val apiKey2 = apiKeyService.create(
      userAccount = project2.permissions.first().user!!,
      scopes = setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW),
      project = project
    )
    val testUser = dbPopulator.createUserIfNotExists("testUser")
    val user2Key = apiKeyService.create(testUser, setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW), project)
    val apiKeyDTO = doCreate("ben")
    var mvcResult = performAuthGet("/api/apiKeys").andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    var set = mvcResult.mapResponseTo<Set<ApiKeyDTO?>>()
    Assertions.assertThat(set).extracting("key").containsExactlyInAnyOrder(apiKeyDTO.key, apiKey1.key, apiKey2.key)
    loginAsUser("testUser")
    mvcResult = performAuthGet("/api/apiKeys").andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    set = mvcResult.mapResponseTo()
    Assertions.assertThat(set).extracting("key").containsExactlyInAnyOrder(user2Key.key)
  }

  @Test
  fun allByProject() {
    val project = dbPopulator.createBase(generateUniqueString())
    val apiKeyDTO = doCreate(project)
    val apiKey1 = apiKeyService.create(project.permissions.first().user!!, setOf(ApiScope.KEYS_EDIT), project)
    val project2 = dbPopulator.createBase(generateUniqueString(), initialUsername)
    val apiKey2 = apiKeyService.create(
      project2.permissions.first().user!!, setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW), project
    )
    val testUser = dbPopulator.createUserIfNotExists("testUser")
    val user2Key = apiKeyService.create(
      testUser, setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW), project2
    )
    var mvcResult = performAuthGet(
      "/api/apiKeys/project/" + project.id
    ).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    var set: Set<ApiKeyDTO> = mvcResult.mapResponseTo()
    Assertions.assertThat(set).extracting("key")
      .containsExactlyInAnyOrder(apiKeyDTO.key, apiKey1.key, apiKey2.key)

    loginAsUser("testUser")
    performAuthGet("/api/apiKeys/project/" + project2.id)
      .andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
    permissionService.grantFullAccessToProject(testUser, project2)
    mvcResult = performAuthGet("/api/apiKeys/project/" + project2.id)
      .andExpect(MockMvcResultMatchers.status().isOk).andReturn()
    set = mapResponse(
      mvcResult,
      TypeFactory.defaultInstance()
        .constructCollectionType(MutableSet::class.java, ApiKeyDTO::class.java)
    )
    Assertions.assertThat(set).extracting("key").containsExactlyInAnyOrder(user2Key.key)
    logout()
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.TRANSLATIONS_EDIT, ApiScope.KEYS_EDIT])
  fun getApiKeyScopes() {
    val scopes = performGet("/api/apiKeys/scopes?ak=" + apiKey.key)
      .andIsOk.andReturn().mapResponseTo<Set<String>>()
    assertThat(scopes).containsAll(setOf(ApiScope.TRANSLATIONS_EDIT.value, ApiScope.KEYS_EDIT.value))
  }

  private fun checkKey(key: String?) {
    Assertions.assertThat(arrayDistinctCount(key!!.chars().boxed().toArray())).isGreaterThan(10)
  }

  private fun <T> arrayDistinctCount(array: Array<T>): Int {
    return Arrays.stream(array).collect(Collectors.toSet()).size
  }
}
