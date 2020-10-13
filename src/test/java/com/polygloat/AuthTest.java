package com.polygloat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polygloat.constants.Message;
import com.polygloat.controllers.AbstractControllerTest;
import com.polygloat.security.AuthController;
import com.polygloat.security.third_party.GithubOAuthDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthTest extends AbstractControllerTest {
    @Value("${polygloat.security.github.authorization-link:#{null}}")
    private String githubLink;

    @Value("${polygloat.security.github.user-link:#{null}}")
    private String githubUserLink;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private AuthController authController;

    private MockMvc authMvc;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    public void setup() {
        authMvc = MockMvcBuilders.standaloneSetup(authController).setControllerAdvice(new ExceptionHandlers()).build();
    }

    @Test
    void generatesTokenForValidUser() throws Exception {
        String response = doAuthentication("ben", "benspassword")
                .getResponse().getContentAsString();

        HashMap result = new ObjectMapper().readValue(response, HashMap.class);

        assertThat(result.get("accessToken")).isNotNull();
        assertThat(result.get("tokenType")).isEqualTo("Bearer");
    }

    @Test
    void DoesNotGenerateTokenForInValidUser() throws Exception {
        MvcResult mvcResult = doAuthentication("bena", "benaspassword");
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(401);
    }


    @Test
    void userWithTokenHasAccess() throws Exception {
        String response = doAuthentication("ben", "benspassword")
                .getResponse().getContentAsString();

        String token = (String) mapper.readValue(response, HashMap.class).get("accessToken");

        MvcResult mvcResult = mvc.perform(get("/api/repositories")
                .accept(MediaType.ALL)
                .header("Authorization", String.format("Bearer %s", token))
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void DoeaNotAuthorizeGithubUserWhenNoEmail() throws Exception {
        GithubOAuthDelegate.GithubUserResponse fakeGithubUser = getGithubUserResponse();

        String accessToken = "fake_access_token";
        GithubOAuthDelegate.GithubEmailResponse githubEmailResponse = getGithubEmailResponse();

        GithubOAuthDelegate.GithubEmailResponse[] emailResponse = {};

        Map<String, String> tokenResponse = getTokenResponse();

        MvcResult mvcResult = authorizeGithubUser(tokenResponse, new ResponseEntity<>(fakeGithubUser, HttpStatus.OK), new ResponseEntity<>(emailResponse, HttpStatus.OK));
        MockHttpServletResponse response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(401);

        assertThat(response.getContentAsString()).contains(Message.THIRD_PARTY_AUTH_NO_EMAIL.getCode());
    }

    @Test
    void DoeaNotAuthorizeGithubUserWhenNoReceivedToken() throws Exception {
        GithubOAuthDelegate.GithubUserResponse fakeGithubUser = getGithubUserResponse();

        String accessToken = "fake_access_token";
        GithubOAuthDelegate.GithubEmailResponse githubEmailResponse = getGithubEmailResponse();

        GithubOAuthDelegate.GithubEmailResponse[] emailResponse = {githubEmailResponse};

        Map<String, String> tokenResponse = null;
        //tokenResponse.put("access_token", accessToken);

        MvcResult mvcResult = authorizeGithubUser(tokenResponse, new ResponseEntity<>(fakeGithubUser, HttpStatus.OK), new ResponseEntity<>(emailResponse, HttpStatus.OK));
        MockHttpServletResponse response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(401);

        assertThat(response.getContentAsString()).contains(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR.getCode());

        tokenResponse = new HashMap<>();
        tokenResponse.put("error", null);
        mvcResult = authorizeGithubUser(tokenResponse, new ResponseEntity<>(fakeGithubUser, HttpStatus.OK), new ResponseEntity<>(emailResponse, HttpStatus.OK));
        response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(401);

        assertThat(response.getContentAsString()).contains(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE.getCode());
    }

    private GithubOAuthDelegate.GithubEmailResponse getGithubEmailResponse() {
        GithubOAuthDelegate.GithubEmailResponse githubEmailResponse = new GithubOAuthDelegate.GithubEmailResponse();
        githubEmailResponse.setEmail("fake_email@email.com");
        githubEmailResponse.setPrimary(true);
        return githubEmailResponse;
    }

    private GithubOAuthDelegate.GithubUserResponse getGithubUserResponse() {
        GithubOAuthDelegate.GithubUserResponse fakeGithubUser = new GithubOAuthDelegate.GithubUserResponse();
        fakeGithubUser.setId("fakeId");
        fakeGithubUser.setName("fakeName");
        return fakeGithubUser;
    }

    @Test
    void authorizesGithubUser() throws Exception {
        GithubOAuthDelegate.GithubUserResponse fakeGithubUser = getGithubUserResponse();

        GithubOAuthDelegate.GithubEmailResponse githubEmailResponse = getGithubEmailResponse();

        GithubOAuthDelegate.GithubEmailResponse[] emailResponse = {githubEmailResponse};

        Map<String, String> tokenResponse = getTokenResponse();

        String response = authorizeGithubUser(tokenResponse, new ResponseEntity<>(fakeGithubUser, HttpStatus.OK), new ResponseEntity<>(emailResponse, HttpStatus.OK))
                .getResponse().getContentAsString();


        HashMap result = new ObjectMapper().readValue(response, HashMap.class);

        assertThat(result.get("accessToken")).isNotNull();
        assertThat(result.get("tokenType")).isEqualTo("Bearer");
    }

    private Map<String, String> getTokenResponse() {
        String accessToken = "fake_access_token";
        Map<String, String> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", accessToken);
        return tokenResponse;
    }


    MvcResult authorizeGithubUser(Map<String, String> tokenResponse,
                                  ResponseEntity<GithubOAuthDelegate.GithubUserResponse> userResponse,
                                  ResponseEntity<GithubOAuthDelegate.GithubEmailResponse[]> emailResponse) throws Exception {
        String receivedCode = "ThiS_Is_Fake_valid_COde";

        Mockito.when(restTemplate.postForObject(eq(githubLink), anyMap(), eq(Map.class))).thenReturn(tokenResponse);

        Mockito.when(restTemplate.exchange(eq(githubUserLink), eq(HttpMethod.GET), ArgumentMatchers.any(), eq(GithubOAuthDelegate.GithubUserResponse.class))).thenReturn(userResponse);

        Mockito.when(restTemplate.exchange(eq(githubUserLink + "/emails"), eq(HttpMethod.GET), ArgumentMatchers.any(), eq(GithubOAuthDelegate.GithubEmailResponse[].class)))
                .thenReturn(emailResponse);

        return authMvc.perform(get("/api/public/authorize_oauth/github/" + receivedCode)
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }


}
