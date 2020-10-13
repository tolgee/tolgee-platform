package com.polygloat.security;


import com.polygloat.constants.ApiScope;
import com.polygloat.controllers.AbstractUserAppApiTest;
import com.polygloat.dtos.request.SetTranslationsDTO;
import com.polygloat.dtos.response.ApiKeyDTO.ApiKeyDTO;
import com.polygloat.model.Repository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static com.polygloat.Assertions.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ApiKeyAuthenticationTest extends AbstractUserAppApiTest {

    @Test
    public void accessWithApiKey_failure() throws Exception {
        MvcResult mvcResult = mvc.perform(get("/uaa/en")).andExpect(status().isForbidden()).andReturn();
        assertThat(mvcResult).error();
    }

    @Test
    public void accessWithApiKey_success() throws Exception {
        Repository base = this.dbPopulator.createBase(generateUniqueString());
        ApiKeyDTO apiKey = this.apiKeyService.createApiKey(base.getCreatedBy(), Set.of(ApiScope.values()), base);
        mvc.perform(get("/uaa/en?ak=" + apiKey.getKey())).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void accessWithApiKey_failure_wrong_key() throws Exception {
        mvc.perform(get("/uaa/en?ak=wrong_api_key")).andExpect(status().isForbidden()).andReturn();
    }

    @Test
    public void accessWithApiKey_failure_api_path() throws Exception {
        Repository base = this.dbPopulator.createBase(generateUniqueString());
        ApiKeyDTO apiKey = this.apiKeyService.createApiKey(base.getCreatedBy(), Set.of(ApiScope.values()), base);
        performAction(buildAction().apiKey(apiKey.getKey()).url("/api/repositories").expectedStatus(HttpStatus.FORBIDDEN));

        mvc.perform(get("/api/repositories")).andExpect(status().isForbidden()).andReturn();
    }

    @Test
    public void accessWithApiKey_listPermissions() {
        ApiKeyDTO apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_VIEW);
        performAction(buildAction().apiKey(apiKey.getKey()).url("/uaa/en").expectedStatus(HttpStatus.OK));

        apiKey = createBaseWithApiKey(ApiScope.SOURCES_EDIT);
        performAction(buildAction().apiKey(apiKey.getKey()).url("/uaa/en").expectedStatus(HttpStatus.FORBIDDEN));
    }

    @Test
    public void accessWithApiKey_editPermissions() {
        ApiKeyDTO apiKey = createBaseWithApiKey(ApiScope.SOURCES_EDIT);

        SetTranslationsDTO translations = SetTranslationsDTO.builder()
                .sourceFullPath("aaaa")
                .translations(Map.of("aaa", "aaa")).build();//just a fake to pass validation

        performAction(buildAction().method(HttpMethod.POST).body(translations).apiKey(apiKey.getKey()).url("/uaa").expectedStatus(HttpStatus.FORBIDDEN));

        apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_EDIT);
        performAction(buildAction().method(HttpMethod.POST).body(translations).apiKey(apiKey.getKey()).url("/uaa").expectedStatus(HttpStatus.NOT_FOUND));
    }


    @Test
    public void accessWithApiKey_getLanguages() {
        ApiKeyDTO apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_VIEW);
        performAction(buildAction().method(HttpMethod.GET).apiKey(apiKey.getKey()).url("/uaa/languages").expectedStatus(HttpStatus.FORBIDDEN));

        apiKey = createBaseWithApiKey(ApiScope.TRANSLATIONS_EDIT);
        performAction(buildAction().method(HttpMethod.GET).apiKey(apiKey.getKey()).url("/uaa/languages").expectedStatus(HttpStatus.OK));
    }
}
