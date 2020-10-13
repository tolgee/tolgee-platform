package com.polygloat.security.third_party;

import com.polygloat.configuration.AppConfiguration;
import com.polygloat.constants.Message;
import com.polygloat.exceptions.AuthenticationException;
import com.polygloat.model.Invitation;
import com.polygloat.model.UserAccount;
import com.polygloat.security.JwtTokenProvider;
import com.polygloat.security.payload.JwtAuthenticationResponse;
import com.polygloat.service.InvitationService;
import com.polygloat.service.UserAccountService;
import com.sun.istack.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GithubOAuthDelegate {
    private final JwtTokenProvider tokenProvider;
    private final UserAccountService userAccountService;
    private final RestTemplate restTemplate;
    private final AppConfiguration appConfiguration;
    private final InvitationService invitationService;

    @Value("${polygloat.security.github.client-secret:#{null}}")
    private String githubClientSecret;

    @Value("${polygloat.security.github.client-id:#{null}}")
    private String githubClientId;

    @Value("${polygloat.security.github.authorization-link:#{null}}")
    private String githubLink;

    @Value("${polygloat.security.github.user-link:#{null}}")
    private String githubUserLink;

    public JwtAuthenticationResponse getTokenResponse(String receivedCode, @Nullable String invitationCode) {
        HashMap<String, String> body = new HashMap<>();
        body.put("client_id", githubClientId);
        body.put("client_secret", githubClientSecret);
        body.put("code", receivedCode);

        //get token to authorize to github api
        @SuppressWarnings("unchecked") Map<String, String> response = restTemplate.postForObject(githubLink, body, Map.class);

        if (response != null && response.containsKey("access_token")) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + response.get("access_token"));
            HttpEntity<String> entity = new HttpEntity<>(null, headers);


            //get github user data
            ResponseEntity<GithubUserResponse> exchange = restTemplate.exchange(githubUserLink, HttpMethod.GET, entity, GithubUserResponse.class);

            if (exchange.getStatusCode() != HttpStatus.OK || exchange.getBody() == null) {
                throw new AuthenticationException(Message.THIRD_PARTY_UNAUTHORIZED);
            }

            GithubUserResponse userResponse = exchange.getBody();

            //get github user emails
            GithubEmailResponse[] emails = restTemplate.exchange(githubUserLink + "/emails", HttpMethod.GET, entity, GithubEmailResponse[].class).getBody();

            if (emails == null) {
                throw new AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL);
            }

            GithubEmailResponse githubEmail = Arrays.stream(emails).filter(GithubEmailResponse::isPrimary).findFirst().orElse(null);

            if (githubEmail == null) {
                throw new AuthenticationException(Message.THIRD_PARTY_AUTH_NO_EMAIL);
            }

            Optional<UserAccount> userAccountOptional = userAccountService.findByThirdParty("github", userResponse.getId());

            UserAccount user = userAccountOptional.orElseGet(() -> {
                userAccountService.getByUserName(githubEmail.getEmail()).ifPresent(u -> {
                    throw new AuthenticationException(Message.USERNAME_ALREADY_EXISTS);
                });

                Invitation invitation = null;

                if (invitationCode == null) {
                    appConfiguration.checkAllowedRegistrations();
                } else {
                    invitation = invitationService.getInvitation(invitationCode);
                }

                UserAccount newUserAccount = new UserAccount();
                newUserAccount.setUsername(githubEmail.getEmail());
                newUserAccount.setName(userResponse.getName());
                newUserAccount.setThirdPartyAuthId(userResponse.getId());
                newUserAccount.setThirdPartyAuthType("github");
                userAccountService.createUser(newUserAccount);

                if (invitation != null) {
                    invitationService.accept(invitation.getCode(), newUserAccount);
                }

                return newUserAccount;
            });

            String jwt = tokenProvider.generateToken(user.getId()).toString();
            return new JwtAuthenticationResponse(jwt);
        }

        if (response == null) {
            throw new AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR);
        }

        if (response.containsKey("error")) {
            throw new AuthenticationException(Message.THIRD_PARTY_AUTH_ERROR_MESSAGE);
        }

        throw new AuthenticationException(Message.THIRD_PARTY_AUTH_UNKNOWN_ERROR);
    }

    public static class GithubEmailResponse {
        @Getter
        @Setter
        private String email;

        @Getter
        @Setter
        private boolean primary;
    }

    public static class GithubUserResponse {
        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private String id;
    }
}
