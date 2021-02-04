package io.tolgee.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.tolgee.AbstractTransactionalTest;
import io.tolgee.configuration.tolgee.AuthenticationProperties;
import io.tolgee.configuration.tolgee.TolgeeProperties;
import io.tolgee.development.DbPopulatorReal;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.UserAccount;
import io.tolgee.repository.KeyRepository;
import io.tolgee.security.InitialPasswordManager;
import io.tolgee.security.payload.LoginRequest;
import io.tolgee.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    protected KeyService keyService;

    @Autowired
    protected LanguageService languageService;

    @Autowired
    protected KeyRepository keyRepository;

    @Autowired
    protected UserAccountService userAccountService;

    @Autowired
    protected ApiKeyService apiKeyService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected InvitationService invitationService;

    @Autowired
    protected TolgeeProperties tolgeeProperties;

    @Autowired
    public ObjectMapper mapper;

    @Autowired
    protected InitialPasswordManager initialPasswordManager;

    @Autowired
    protected ScreenshotService screenshotService;

    protected String initialUsername;

    protected String initialPassword;

    @Autowired
    private void initInitialUser(AuthenticationProperties authenticationProperties) {
        initialUsername = authenticationProperties.getInitialUsername();
        initialPassword = initialPasswordManager.getInitialPassword();
    }

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


    protected <C extends Collection<E>, E> C mapResponse(MvcResult result, Class<C> collectionType, Class<E> elementType) {
        try {
            return mapper.readValue(
                    result.getResponse().getContentAsString(),
                    TypeFactory.defaultInstance().constructCollectionType(collectionType, elementType)
            );
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
