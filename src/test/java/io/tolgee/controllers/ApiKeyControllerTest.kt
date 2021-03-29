package io.tolgee.controllers

import com.fasterxml.jackson.databind.type.TypeFactory
import io.tolgee.ITest
import io.tolgee.annotations.ApiKeyAccessTestMethod
import io.tolgee.annotations.RepositoryApiKeyAuthTestMethod
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.constants.ApiScope
import io.tolgee.dtos.request.CreateApiKeyDTO
import io.tolgee.dtos.request.EditApiKeyDTO
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.model.Repository
import org.assertj.core.api.Assertions
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testng.annotations.Test
import java.util.*
import java.util.stream.Collectors

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyControllerTest : RepositoryAuthControllerTest(), ITest {

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

    private fun doCreate(repository: Repository = dbPopulator.createBase(generateUniqueString())): ApiKeyDTO {
        val requestDto = CreateApiKeyDTO.builder()
                .repositoryId(repository.id)
                .scopes(setOf(ApiScope.TRANSLATIONS_VIEW, ApiScope.KEYS_EDIT))
                .build()
        val mvcResult = performAuthPost("/api/apiKeys", requestDto).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        return mapResponse(mvcResult, ApiKeyDTO::class.java)
    }

    @Test
    fun create_failure_no_scopes() {
        var base = dbPopulator.createBase(generateUniqueString())
        val requestDto = CreateApiKeyDTO.builder().repositoryId(base.id).scopes(setOf()).build()
        val mvcResult = performAuthPost("/api/apiKeys", requestDto).andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        assertThat(mvcResult).error().isStandardValidation.onField("scopes").isEqualTo("must not be empty")
        assertThat(mvcResult).error().isStandardValidation.errorCount().isEqualTo(1)
    }

    @Test
    fun create_failure_no_repository() {
        val requestDto = CreateApiKeyDTO.builder().scopes(setOf(ApiScope.TRANSLATIONS_VIEW)).build()
        val mvcResult = performAuthPost("/api/apiKeys", requestDto).andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        assertThat(mvcResult).error().isStandardValidation.onField("repositoryId").isEqualTo("must not be null")
        assertThat(mvcResult).error().isStandardValidation.errorCount().isEqualTo(1)
    }

    @Test
    fun edit_success() {
        val apiKeyDTO = doCreate()
        val newScopes = setOf(ApiScope.TRANSLATIONS_EDIT)
        val editDto = EditApiKeyDTO.builder().id(apiKeyDTO.id).scopes(newScopes).build()
        performAuthPost("/api/apiKeys/edit", editDto).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        val apiKey = apiKeyService.getApiKey(apiKeyDTO.id)
        Assertions.assertThat(apiKey).isPresent
        Assertions.assertThat(apiKey.get().scopesEnum).isEqualTo(newScopes)
    }

    @Test
    fun edit_failure_no_scopes() {
        val newScopes: Set<ApiScope> = setOf()
        val apiKeyDTO = doCreate()
        val editDto = EditApiKeyDTO.builder().id(apiKeyDTO.id).scopes(newScopes).build()
        val mvcResult = performAuthPost("/api/apiKeys/edit", editDto).andExpect(MockMvcResultMatchers.status().isBadRequest).andReturn()
        assertThat(mvcResult).error().isStandardValidation.onField("scopes").isEqualTo("must not be empty")
    }

    @Test
    fun getAllByUser() {
        val repository = dbPopulator.createBase(generateUniqueString(), "ben")
        logAsUser("ben", initialPassword)
        val apiKey1 = apiKeyService.createApiKey(repository.permissions.first().user, setOf(ApiScope.KEYS_EDIT), repository)
        val repository2 = dbPopulator.createBase(generateUniqueString(), "ben")
        val apiKey2 = apiKeyService.createApiKey(repository2.permissions.first().user, setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW), repository)
        val testUser = dbPopulator.createUser("testUser")
        val user2Key = apiKeyService.createApiKey(testUser, setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW), repository)
        val apiKeyDTO = doCreate("ben")
        var mvcResult = performAuthGet("/api/apiKeys").andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        var set = mapResponse<Set<ApiKeyDTO?>>(mvcResult, TypeFactory.defaultInstance().constructCollectionType(MutableSet::class.java, ApiKeyDTO::class.java))
        Assertions.assertThat(set).extracting("key").containsExactlyInAnyOrder(apiKeyDTO.key, apiKey1.key, apiKey2.key)
        logAsUser("testUser", initialPassword)
        mvcResult = performAuthGet("/api/apiKeys").andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        set = mapResponse(mvcResult, TypeFactory.defaultInstance().constructCollectionType(MutableSet::class.java, ApiKeyDTO::class.java))
        Assertions.assertThat(set).extracting("key").containsExactlyInAnyOrder(user2Key.key)
    }

    @Test
    fun allByRepository() {
        val repository = dbPopulator.createBase(generateUniqueString())
        val apiKeyDTO = doCreate(repository)
        val apiKey1 = apiKeyService.createApiKey(repository.permissions.first().user, setOf(ApiScope.KEYS_EDIT), repository)
        val repository2 = dbPopulator.createBase(generateUniqueString(), initialUsername)
        val apiKey2 = apiKeyService.createApiKey(repository2.permissions.first().user, setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW), repository)
        val testUser = dbPopulator.createUser("testUser")
        val user2Key = apiKeyService.createApiKey(testUser, setOf(ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW), repository2)
        var mvcResult = performAuthGet("/api/apiKeys/repository/" + repository.id).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        var set: Set<ApiKeyDTO> = mvcResult.mapResponseTo()
        Assertions.assertThat(set).extracting("key").containsExactlyInAnyOrder(apiKeyDTO.key, apiKey1.key, apiKey2.key)
        logAsUser("testUser", initialPassword)
        performAuthGet("/api/apiKeys/repository/" + repository2.id).andExpect(MockMvcResultMatchers.status().isForbidden).andReturn()
        permissionService.grantFullAccessToRepo(testUser, repository2)
        mvcResult = performAuthGet("/api/apiKeys/repository/" + repository2.id).andExpect(MockMvcResultMatchers.status().isOk).andReturn()
        set = mapResponse(mvcResult, TypeFactory.defaultInstance().constructCollectionType(MutableSet::class.java, ApiKeyDTO::class.java))
        Assertions.assertThat(set).extracting("key").containsExactlyInAnyOrder(user2Key.key)
        logout()
    }

    @Test
    @RepositoryApiKeyAuthTestMethod(scopes = [ApiScope.TRANSLATIONS_EDIT, ApiScope.KEYS_EDIT])
    fun getApiKeyScopes() {
        val scopes = performGet("/api/apiKeys/scopes?ak=" + apiKey.key).andIsOk.andReturn().mapResponseTo<Set<String>>()
        assertThat(scopes).containsAll(setOf(ApiScope.TRANSLATIONS_EDIT.value, ApiScope.KEYS_EDIT.value))
    }

    private fun checkKey(key: String?) {
        Assertions.assertThat(arrayDistinctCount(key!!.chars().boxed().toArray())).isGreaterThan(10)
    }

    private fun <T> arrayDistinctCount(array: Array<T>): Int {
        return Arrays.stream(array).collect(Collectors.toSet()).size
    }
}
