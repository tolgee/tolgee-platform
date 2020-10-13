package com.polygloat.controllers;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.polygloat.constants.ApiScope;
import com.polygloat.development.DbPopulatorReal;
import com.polygloat.dtos.request.CreateApiKeyDTO;
import com.polygloat.dtos.request.EditApiKeyDTO;
import com.polygloat.dtos.response.ApiKeyDTO.ApiKeyDTO;
import com.polygloat.model.ApiKey;
import com.polygloat.model.Repository;
import com.polygloat.model.UserAccount;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.polygloat.Assertions.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiKeyControllerTest extends SignedInControllerTest implements ITest {

    @Test()
    void create_success() throws Exception {
        ApiKeyDTO apiKeyDTO = doCreate();
        Optional<ApiKey> apiKey = apiKeyService.getApiKey(apiKeyDTO.getKey());
        assertThat(apiKey).isPresent();
        checkKey(apiKey.get().getKey());
    }

    private ApiKeyDTO doCreate() throws Exception {
        return doCreate(dbPopulator.createBase(generateUniqueString()));
    }

    private ApiKeyDTO doCreate(Repository repository) throws Exception {
        CreateApiKeyDTO requestDto = CreateApiKeyDTO.builder().repositoryId(repository.getId()).scopes(Set.of(ApiScope.TRANSLATIONS_VIEW, ApiScope.SOURCES_EDIT)).build();
        MvcResult mvcResult = performPost("/api/apiKeys", requestDto).andExpect(status().isOk()).andReturn();
        return mapResponse(mvcResult, ApiKeyDTO.class);
    }

    @Test()
    void create_failure_no_scopes() throws Exception {
        Repository repository = dbPopulator.createBase(generateUniqueString());
        CreateApiKeyDTO requestDto = CreateApiKeyDTO.builder().repositoryId(repository.getId()).scopes(Set.of()).build();
        MvcResult mvcResult = performPost("/api/apiKeys", requestDto).andExpect(status().isBadRequest()).andReturn();
        assertThat(mvcResult).error().isStandardValidation().onField("scopes").isEqualTo("must not be empty");
        assertThat(mvcResult).error().isStandardValidation().errorCount().isEqualTo(1);
    }

    @Test()
    void create_failure_no_repository() throws Exception {
        CreateApiKeyDTO requestDto = CreateApiKeyDTO.builder().scopes(Set.of(ApiScope.TRANSLATIONS_VIEW)).build();
        MvcResult mvcResult = performPost("/api/apiKeys", requestDto).andExpect(status().isBadRequest()).andReturn();
        assertThat(mvcResult).error().isStandardValidation().onField("repositoryId").isEqualTo("must not be null");
        assertThat(mvcResult).error().isStandardValidation().errorCount().isEqualTo(1);
    }

    @Test()
    void edit_success() throws Exception {
        ApiKeyDTO apiKeyDTO = doCreate();
        Set<ApiScope> newScopes = Set.of(ApiScope.TRANSLATIONS_EDIT);
        EditApiKeyDTO editDto = EditApiKeyDTO.builder().id(apiKeyDTO.getId()).scopes(newScopes).build();
        performPost("/api/apiKeys/edit", editDto).andExpect(status().isOk()).andReturn();
        Optional<ApiKey> apiKey = apiKeyService.getApiKey(apiKeyDTO.getId());
        assertThat(apiKey).isPresent();
        assertThat(apiKey.get().getScopes()).isEqualTo(newScopes);
    }

    @Test()
    void edit_failure_no_scopes() throws Exception {
        Set<ApiScope> newScopes = Set.of();
        ApiKeyDTO apiKeyDTO = doCreate();
        EditApiKeyDTO editDto = EditApiKeyDTO.builder().id(apiKeyDTO.getId()).scopes(newScopes).build();
        MvcResult mvcResult = performPost("/api/apiKeys/edit", editDto).andExpect(status().isBadRequest()).andReturn();
        assertThat(mvcResult).error().isStandardValidation().onField("scopes").isEqualTo("must not be empty");
    }

    @Test()
    void getAllByUser() throws Exception {
        Repository repository = dbPopulator.createBase(generateUniqueString());
        ApiKeyDTO apiKey1 = apiKeyService.createApiKey(repository.getCreatedBy(), Set.of(ApiScope.SOURCES_EDIT), repository);
        Repository repository2 = dbPopulator.createBase(generateUniqueString(), DbPopulatorReal.DEFAULT_USERNAME);
        ApiKeyDTO apiKey2 = apiKeyService.createApiKey(repository2.getCreatedBy(), Set.of(ApiScope.SOURCES_EDIT, ApiScope.TRANSLATIONS_VIEW), repository);
        UserAccount testUser = dbPopulator.createUser("testUser");
        ApiKeyDTO user2Key = apiKeyService.createApiKey(testUser, Set.of(ApiScope.SOURCES_EDIT, ApiScope.TRANSLATIONS_VIEW), repository);

        ApiKeyDTO apiKeyDTO = doCreate();

        MvcResult mvcResult = performGet("/api/apiKeys").andExpect(status().isOk()).andReturn();
        Set<ApiKeyDTO> set = mapResponse(mvcResult, TypeFactory.defaultInstance().constructCollectionType(Set.class, ApiKeyDTO.class));
        assertThat(set).extracting("key").containsExactlyInAnyOrder(apiKeyDTO.getKey(), apiKey1.getKey(), apiKey2.getKey());

        logAsUser("testUser", "testUser");
        mvcResult = performGet("/api/apiKeys").andExpect(status().isOk()).andReturn();
        set = mapResponse(mvcResult, TypeFactory.defaultInstance().constructCollectionType(Set.class, ApiKeyDTO.class));
        assertThat(set).extracting("key").containsExactlyInAnyOrder(user2Key.getKey());
    }

    @Test()
    void getAllByRepository() throws Exception {
        Repository repository = dbPopulator.createBase(generateUniqueString());
        ApiKeyDTO apiKeyDTO = doCreate(repository);
        ApiKeyDTO apiKey1 = apiKeyService.createApiKey(repository.getCreatedBy(), Set.of(ApiScope.SOURCES_EDIT), repository);
        Repository repository2 = dbPopulator.createBase(generateUniqueString(), DbPopulatorReal.DEFAULT_USERNAME);
        ApiKeyDTO apiKey2 = apiKeyService.createApiKey(repository2.getCreatedBy(), Set.of(ApiScope.SOURCES_EDIT, ApiScope.TRANSLATIONS_VIEW), repository);
        UserAccount testUser = dbPopulator.createUser("testUser");
        ApiKeyDTO user2Key = apiKeyService.createApiKey(testUser, Set.of(ApiScope.SOURCES_EDIT, ApiScope.TRANSLATIONS_VIEW), repository2);

        MvcResult mvcResult = performGet("/api/apiKeys/repository/" + repository.getId()).andExpect(status().isOk()).andReturn();

        @SuppressWarnings("unchecked") Set<ApiKeyDTO> set = mapResponse(mvcResult, Set.class, ApiKeyDTO.class);
        assertThat(set).extracting("key").containsExactlyInAnyOrder(apiKeyDTO.getKey(), apiKey1.getKey(), apiKey2.getKey());

        logAsUser("testUser", "testUser");
        performGet("/api/apiKeys/repository/" + repository2.getId()).andExpect(status().isForbidden()).andReturn();

        permissionService.grantFullAccessToRepo(testUser, repository2);

        mvcResult = performGet("/api/apiKeys/repository/" + repository2.getId()).andExpect(status().isOk()).andReturn();
        set = mapResponse(mvcResult, TypeFactory.defaultInstance().constructCollectionType(Set.class, ApiKeyDTO.class));
        assertThat(set).extracting("key").containsExactlyInAnyOrder(user2Key.getKey());
        logout();
    }

    private void checkKey(String key) {
        assertThat(arrayDistinctCount(key.chars().boxed().toArray())).isGreaterThan(10);
    }

    private <T> int arrayDistinctCount(T[] array) {
        return Arrays.stream(array).collect(Collectors.toSet()).size();
    }
}
