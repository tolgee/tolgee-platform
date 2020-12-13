package io.polygloat.controllers

import com.fasterxml.jackson.databind.node.TextNode
import com.unboundid.util.Base64
import io.polygloat.configuration.polygloat.PolygloatProperties
import io.polygloat.constants.Message
import io.polygloat.dtos.request.ResetPassword
import io.polygloat.dtos.request.ResetPasswordRequest
import io.polygloat.dtos.request.SignUpDto
import io.polygloat.exceptions.AuthenticationException
import io.polygloat.exceptions.BadRequestException
import io.polygloat.exceptions.NotFoundException
import io.polygloat.model.Invitation
import io.polygloat.model.UserAccount
import io.polygloat.security.JwtTokenProvider
import io.polygloat.security.payload.ApiResponse
import io.polygloat.security.payload.JwtAuthenticationResponse
import io.polygloat.security.payload.LoginRequest
import io.polygloat.security.third_party.GithubOAuthDelegate
import io.polygloat.service.InvitationService
import io.polygloat.service.UserAccountService
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.function.Supplier
import javax.validation.Valid

@RestController
@RequestMapping("/api/public")
open class AuthController(private val authenticationManager: AuthenticationManager,
                          private val tokenProvider: JwtTokenProvider,
                          private val githubOAuthDelegate: GithubOAuthDelegate,
                          private val properties: PolygloatProperties,
                          private val userAccountService: UserAccountService,
                          private val mailSender: JavaMailSender,
                          private val invitationService: InvitationService
) {
    @PostMapping("/generatetoken")
    open fun authenticateUser(@RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
        if (loginRequest.username.isEmpty() || loginRequest.password.isEmpty()) {
            return ResponseEntity(ApiResponse(false, Message.USERNAME_OR_PASSWORD_INVALID.code),
                    HttpStatus.BAD_REQUEST)
        }
        if (properties.authentication.ldap.enabled && properties.authentication.nativeEnabled) {
            //todo: validate properties
            throw RuntimeException("Can not use native auth and ldap auth in the same time")
        }
        var jwt: String? = null
        if (properties.authentication.ldap.enabled) {
            jwt = doLdapAuthorization(loginRequest)
        }
        if (properties.authentication.nativeEnabled) {
            jwt = doNativeAuth(loginRequest)
        }
        if (jwt == null) {
            //todo: validate properties
            throw RuntimeException("Authentication method not configured")
        }
        return ResponseEntity.ok(JwtAuthenticationResponse(jwt))
    }


    @PostMapping("/reset_password_request")
    open fun resetPasswordRequest(@RequestBody request: ResetPasswordRequest) {
        val userAccount = userAccountService.getByUserName(request.email).orElse(null) ?: return
        val code = RandomStringUtils.randomAlphabetic(50)
        userAccountService.setResetPasswordCode(userAccount, code)
        val message = SimpleMailMessage()
        message.setTo(request.email!!)
        message.subject = "Password reset"
        val url = request.callbackUrl + "/" + Base64.encode(code + "," + request.email)
        message.text = """Hello!
 To reset your password click this link: 
$url

 If you have not requested password reset, please just ignore this e-mail."""
        message.from = properties.smtp.from
        mailSender.send(message)
    }

    @GetMapping("/reset_password_validate/{email}/{code}")
    open fun resetPasswordValidate(@PathVariable("code") code: String, @PathVariable("email") email: String) {
        validateEmailCode(code, email)
    }

    @PostMapping("/reset_password_set")
    open fun resetPasswordSet(@RequestBody request: ResetPassword) {
        val userAccount = validateEmailCode(request.code!!, request.email!!)
        userAccountService.setUserPassword(userAccount, request.password)
        userAccountService.removeResetCode(userAccount)
    }

    @PostMapping("/sign_up")
    @Transactional
    open fun signUp(@RequestBody @Valid request: SignUpDto?): JwtAuthenticationResponse {
        var invitation: Invitation? = null
        if (request!!.invitationCode == null || request.invitationCode.isEmpty()) {
            properties.authentication.checkAllowedRegistrations()
        } else {
            invitation = invitationService.getInvitation(request.invitationCode) //it throws an exception
        }
        userAccountService.getByUserName(request.email).ifPresent {
            throw BadRequestException(Message.USERNAME_ALREADY_EXISTS)
        }
        val user = userAccountService.createUser(request)
        if (invitation != null) {
            invitationService.accept(invitation.code, user)
        }
        return JwtAuthenticationResponse(tokenProvider.generateToken(user.id).toString())
    }

    @PostMapping(value = ["/validate_email"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    open fun validateEmail(@RequestBody email: TextNode): Boolean {
        return userAccountService.getByUserName(email.asText()).isEmpty
    }


    @GetMapping("/authorize_oauth/{serviceType}/{code}")
    open fun authenticateUser(@PathVariable("serviceType") serviceType: String?,
                              @PathVariable("code") code: String?,
                              @RequestParam(value = "invitationCode", required = false) invitationCode: String?): JwtAuthenticationResponse {
        return githubOAuthDelegate.getTokenResponse(code, invitationCode)
    }

    private fun doNativeAuth(loginRequest: LoginRequest): String {
        val userAccount = userAccountService.getByUserName(loginRequest.username).orElseThrow {
            AuthenticationException(Message.BAD_CREDENTIALS)
        }
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        val matches = bCryptPasswordEncoder.matches(loginRequest.password, userAccount.password)
        if (!matches) {
            throw AuthenticationException(Message.BAD_CREDENTIALS)
        }
        return tokenProvider.generateToken(userAccount.id).toString()
    }

    private fun doLdapAuthorization(loginRequest: LoginRequest): String {
        return try {
            val authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken(
                            loginRequest.username,
                            loginRequest.password
                    )
            )
            val userPrincipal = authentication.principal as LdapUserDetailsImpl
            val userAccountEntity = userAccountService.getByUserName(userPrincipal.username).orElseGet {
                val userAccount = UserAccount()
                userAccount.username = userPrincipal.username
                userAccountService.createUser(userAccount)
                userAccount
            }
            tokenProvider.generateToken(userAccountEntity.id).toString()
        } catch (e: BadCredentialsException) {
            throw AuthenticationException(Message.BAD_CREDENTIALS)
        }
    }

    private fun validateEmailCode(code: String, email: String): UserAccount {
        val userAccount = userAccountService.getByUserName(email).orElseThrow({ NotFoundException() })
                ?: throw BadRequestException(Message.BAD_CREDENTIALS)
        val resetCodeValid = userAccountService.isResetCodeValid(userAccount, code)
        if (!resetCodeValid) {
            throw BadRequestException(Message.BAD_CREDENTIALS)
        }
        return userAccount
    }

}