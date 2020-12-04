package io.polygloat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.polygloat.constants.Message;
import io.polygloat.controllers.AbstractControllerTest;
import io.polygloat.controllers.AuthController;
import io.polygloat.security.third_party.GithubOAuthDelegate;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthTest extends AbstractControllerTest {
    @Autowired
    private AuthController authController;

    @MockBean
    @Autowired
    private RestTemplate restTemplate;

    private MockMvc authMvc;

    @BeforeClass
    public void setup() {
        dbPopulator.createBase(generateUniqueString());
        authMvc = MockMvcBuilders.standaloneSetup(authController).setControllerAdvice(new ExceptionHandlers()).build();
    }

    @Test
    void generatesTokenForValidUser() throws Exception {
        String response = doAuthentication(initialUsername, initialPassword)
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
        String response = doAuthentication(initialUsername, initialPassword)
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
    void doesNotAuthorizeGithubUserWhenNoEmail() throws Exception {
        GithubOAuthDelegate.GithubUserResponse fakeGithubUser = getGithubUserResponse();

        GithubOAuthDelegate.GithubEmailResponse[] emailResponse = {};

        Map<String, String> tokenResponse = getTokenResponse();

        MvcResult mvcResult = authorizeGithubUser(tokenResponse, new ResponseEntity<>(fakeGithubUser, HttpStatus.OK), new ResponseEntity<>(emailResponse, HttpStatus.OK));
        MockHttpServletResponse response = mvcResult.getResponse();
        assertThat(response.getStatus()).isEqualTo(401);

        assertThat(response.getContentAsString()).contains(Message.THIRD_PARTY_AUTH_NO_EMAIL.getCode());
    }

    @Test
    void doesNotAuthorizeGithubUserWhenNoReceivedToken() throws Exception {
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

        var githubConf = polygloatProperties.getAuthentication().getGithub();

        Mockito.when(restTemplate.postForObject(eq(githubConf.getAuthorizationUrl()), anyMap(), eq(Map.class))).thenReturn(tokenResponse);

        Mockito.when(restTemplate.exchange(eq(githubConf.getUserUrl()), eq(HttpMethod.GET), ArgumentMatchers.any(), eq(GithubOAuthDelegate.GithubUserResponse.class))).thenReturn(userResponse);

        Mockito.when(restTemplate.exchange(eq(githubConf.getUserUrl() + "/emails"), eq(HttpMethod.GET), ArgumentMatchers.any(), eq(GithubOAuthDelegate.GithubEmailResponse[].class)))
                .thenReturn(emailResponse);

        return authMvc.perform(get("/api/public/authorize_oauth/github/" + receivedCode)
                .accept(MediaType.ALL)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }


}
