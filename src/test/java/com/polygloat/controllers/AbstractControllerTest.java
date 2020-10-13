package com.polygloat.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.polygloat.AbstractTransactionalTest;
import com.polygloat.development.DbPopulatorReal;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.UserAccount;
import com.polygloat.repository.SourceRepository;
import com.polygloat.security.payload.LoginRequest;
import com.polygloat.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
public abstract class AbstractControllerTest extends AbstractTransactionalTest implements ITest {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") //false positive
    @Autowired
    protected MockMvc mvc;

    @Autowired
    public DbPopulatorReal dbPopulator;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected TranslationService translationService;

    @Autowired
    protected LanguageService languageService;

    @Autowired
    protected SourceRepository sourceRepository;

    @Autowired
    protected UserAccountService userAccountService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    public ObjectMapper mapper;

    <T> T decodeJson(String json, Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected DefaultAuthenticationResult login(String userName, String password) throws Exception {
        String response = doAuthentication(userName, password)
                .getResponse().getContentAsString();

        UserAccount userAccount = userAccountService.getByUserName(userName).orElseThrow(NotFoundException::new);

        return new DefaultAuthenticationResult((String) mapper.readValue(response, HashMap.class).get("accessToken"), userAccount);
    }

    protected MvcResult doAuthentication(String username, String password) throws Exception {

        LoginRequest request = new LoginRequest();

        request.setUsername(username);
        request.setPassword(password);

        String jsonRequest = mapper.writeValueAsString(request);

        return mvc.perform(post("/api/public/generatetoken")
                .content(jsonRequest)
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    protected <T> T mapResponse(MvcResult result, JavaType type) {
        try {
            return mapper.readValue(result.getResponse().getContentAsString(), type);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T mapResponse(MvcResult result, Class<T> clazz) {
        try {
            return mapper.readValue(result.getResponse().getContentAsString(), clazz);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    @SuppressWarnings("unchecked")
    protected <C extends Collection<E>, E> C mapResponse(MvcResult result, Class<C> collectionType, Class<E> elementType) {
        try {
            return (C) mapper.readValue(
                    result.getResponse().getContentAsString(),
                    TypeFactory.defaultInstance().constructCollectionType(collectionType, elementType)
            );
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
