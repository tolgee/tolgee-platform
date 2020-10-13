package com.polygloat.security;

import com.fasterxml.jackson.databind.node.TextNode;
import com.polygloat.configuration.AppConfiguration;
import com.polygloat.constants.Message;
import com.polygloat.dtos.request.ResetPassword;
import com.polygloat.dtos.request.ResetPasswordRequest;
import com.polygloat.dtos.request.SignUpDto;
import com.polygloat.exceptions.AuthenticationException;
import com.polygloat.exceptions.BadRequestException;
import com.polygloat.exceptions.NotFoundException;
import com.polygloat.model.Invitation;
import com.polygloat.model.UserAccount;
import com.polygloat.security.payload.ApiResponse;
import com.polygloat.security.payload.JwtAuthenticationResponse;
import com.polygloat.security.payload.LoginRequest;
import com.polygloat.security.third_party.GithubOAuthDelegate;
import com.polygloat.service.InvitationService;
import com.polygloat.service.UserAccountService;
import com.unboundid.util.Base64;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final GithubOAuthDelegate githubOAuthDelegate;
    private final AppConfiguration appConfiguration;
    private final UserAccountService userAccountService;
    private final JavaMailSender mailSender;
    private final InvitationService invitationService;

    @PostMapping("/generatetoken")
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername().isEmpty() || loginRequest.getPassword().isEmpty()) {
            return new ResponseEntity(new ApiResponse(false, Message.USERNAME_OR_PASSWORD_INVALID.getCode()),
                    HttpStatus.BAD_REQUEST);
        }

        if (appConfiguration.isLdapAuthentication() && appConfiguration.isNativeAuth()) {
            //todo: validate properties
            throw new RuntimeException("Can not use native auth and ldap auth in the same time");
        }

        String jwt = null;
        if (appConfiguration.isLdapAuthentication()) {
            jwt = doLdapAuthorization(loginRequest);
        }

        if (appConfiguration.isNativeAuth()) {
            jwt = doNativeAuth(loginRequest);
        }

        if (jwt == null) {
            //todo: validate properties
            throw new RuntimeException("Authentication method not configured");
        }

        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    private String doNativeAuth(LoginRequest loginRequest) {
        UserAccount userAccount = this.userAccountService.getByUserName(loginRequest.getUsername()).orElseThrow(() -> new AuthenticationException(Message.BAD_CREDENTIALS));
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        boolean matches = bCryptPasswordEncoder.matches(loginRequest.getPassword(), userAccount.getPassword());

        if (!matches) {
            throw new AuthenticationException(Message.BAD_CREDENTIALS);
        }

        return tokenProvider.generateToken(userAccount.getId()).toString();
    }

    private String doLdapAuthorization(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            LdapUserDetailsImpl userPrincipal = (LdapUserDetailsImpl) authentication.getPrincipal();

            UserAccount userAccountEntity = userAccountService.getByUserName(userPrincipal.getUsername()).orElseGet(() -> {
                UserAccount userAccount = new UserAccount();
                userAccount.setUsername(userPrincipal.getUsername());
                userAccountService.createUser(userAccount);
                return userAccount;
            });

            return tokenProvider.generateToken(userAccountEntity.getId()).toString();

        } catch (BadCredentialsException e) {
            throw new AuthenticationException(Message.BAD_CREDENTIALS);
        }
    }


    @PostMapping("/reset_password_request")
    public void resetPasswordRequest(@RequestBody ResetPasswordRequest request) {
        UserAccount userAccount = userAccountService.getByUserName(request.getEmail()).orElse(null);

        if (userAccount == null) {
            return;
        }

        String code = RandomStringUtils.randomAlphabetic(50);
        userAccountService.setResetPasswordCode(userAccount, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getEmail());
        message.setSubject("Password reset");

        String url = request.getCallbackUrl() + "/" + Base64.encode(code + "," + request.getEmail());

        message.setText("Hello!\n To reset your password click this link: \n" + url + "\n" + "\n" +
                " If you have not requested password reset, please just ignore this e-mail.");
        message.setFrom(appConfiguration.getMailFrom());
        mailSender.send(message);
    }

    @GetMapping("/reset_password_validate/{email}/{code}")
    public void resetPasswordValidate(@PathVariable("code") String code, @PathVariable("email") String email) {
        this.validateEmailCode(code, email);
    }

    @PostMapping("/reset_password_set")
    public void resetPasswordSet(@RequestBody ResetPassword request) {
        UserAccount userAccount = validateEmailCode(request.getCode(), request.getEmail());
        userAccountService.setUserPassword(userAccount, request.getPassword());
        userAccountService.removeResetCode(userAccount);
    }

    @PostMapping("/sign_up")
    @Transactional
    public JwtAuthenticationResponse signUp(@RequestBody @Valid SignUpDto request) {
        Invitation invitation = null;

        if (request.getInvitationCode() == null || request.getInvitationCode().isEmpty()) {
            appConfiguration.checkAllowedRegistrations();
        } else {
            invitation = invitationService.getInvitation(request.getInvitationCode());//it throws an exception
        }

        userAccountService.getByUserName(request.getEmail()).ifPresent(u -> {
            throw new BadRequestException(Message.USERNAME_ALREADY_EXISTS);
        });

        UserAccount user = userAccountService.createUser(request);

        if (invitation != null) {
            invitationService.accept(invitation.getCode(), user);
        }

        return new JwtAuthenticationResponse(this.tokenProvider.generateToken(user.getId()).toString());
    }

    @PostMapping(value = "/validate_email", consumes = MediaType.APPLICATION_JSON_VALUE)
    public boolean validateEmail(@RequestBody TextNode email) {
        return userAccountService.getByUserName(email.asText()).isEmpty();
    }

    private UserAccount validateEmailCode(String code, String email) {
        UserAccount userAccount = userAccountService.getByUserName(email).orElseThrow(NotFoundException::new);

        if (userAccount == null) {
            throw new BadRequestException(Message.BAD_CREDENTIALS);
        }

        boolean resetCodeValid = userAccountService.isResetCodeValid(userAccount, code);

        if (!resetCodeValid) {
            throw new BadRequestException(Message.BAD_CREDENTIALS);
        }

        return userAccount;
    }


    @GetMapping("/authorize_oauth/{serviceType}/{code}")
    public JwtAuthenticationResponse authenticateUser(@PathVariable("serviceType") String serviceType,
                                                      @PathVariable("code") String code,
                                                      @RequestParam(value = "invitationCode", required = false) String invitationCode) {
        return githubOAuthDelegate.getTokenResponse(code, invitationCode);
    }
}

