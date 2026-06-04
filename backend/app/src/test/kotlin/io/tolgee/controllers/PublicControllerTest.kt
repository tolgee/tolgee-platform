package io.tolgee.controllers

import com.posthog.server.PostHog
import io.tolgee.constants.Message
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertResponse
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsUnauthorized
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import kotlin.properties.Delegates

@AutoConfigureMockMvc
class PublicControllerTest : AbstractControllerTest() {
  private var canCreateOrganizations by Delegates.notNull<Boolean>()
  private var registrationsAllowed by Delegates.notNull<Boolean>()
  private var blockDisposableEmails by Delegates.notNull<Boolean>()
  private var blockEmailAliases by Delegates.notNull<Boolean>()

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
    canCreateOrganizations = tolgeeProperties.authentication.userCanCreateOrganizations
    registrationsAllowed = tolgeeProperties.authentication.registrationsAllowed
    blockDisposableEmails = tolgeeProperties.authentication.blockDisposableEmails
    blockEmailAliases = tolgeeProperties.authentication.blockEmailAliases
  }

  @AfterEach
  fun tearDown() {
    tolgeeProperties.authentication.userCanCreateOrganizations = canCreateOrganizations
    tolgeeProperties.authentication.registrationsAllowed = registrationsAllowed
    tolgeeProperties.authentication.blockDisposableEmails = blockDisposableEmails
    tolgeeProperties.authentication.blockEmailAliases = blockEmailAliases
    tolgeeProperties.authentication.blockedEmailDomains.clear()
    tolgeeProperties.authentication.allowedEmailDomains.clear()
  }

  @Autowired
  lateinit var postHog: PostHog

  @Test
  fun `creates organization`() {
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "aaaa@aaaa.com",
        organizationName = "Hello",
      )
    performPost("/api/public/sign_up", dto).andIsOk
    assertThat(organizationRepository.findAllByName("Hello")).hasSize(1)
  }

  @Test
  fun `creates organization when no invitation`() {
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "aaaa@aaaa.com")
    performPost("/api/public/sign_up", dto).andIsOk
    assertThat(organizationRepository.findAllByName("Pavel Novak")).hasSize(1)
  }

  @Test
  fun `stores the username lowercased on sign up`() {
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "Pavel.Novak@Example.COM")
    performPost("/api/public/sign_up", dto).andIsOk
    assertThat(userAccountService.findActive("pavel.novak@example.com")!!.username)
      .isEqualTo("pavel.novak@example.com")
  }

  @Test
  fun `rejects sign up with the same email in different casing`() {
    performPost(
      "/api/public/sign_up",
      SignUpDto(name = "A", password = "aaaaaaaaa", email = "dup.case@example.com"),
    ).andIsOk
    performPost(
      "/api/public/sign_up",
      SignUpDto(name = "B", password = "aaaaaaaaa", email = "Dup.Case@Example.com"),
    ).andIsBadRequest
  }

  @Test
  fun `logs event to external monitor`() {
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "aaaa@aaaa.com")
    performPost(
      "/api/public/sign_up",
      dto,
      HttpHeaders().also {
        it["X-Tolgee-Utm"] = "eyJ1dG1faGVsbG8iOiJoZWxsbyJ9"
        // hypothetically every endpoint might be triggered from SDK
        it["X-Tolgee-SDK-Type"] = "Unreal"
        it["X-Tolgee-SDK-Version"] = "1.0.0"
      },
    ).andIsOk

    val params = assertPostHogEventReported(postHog, "SIGN_UP")
    params["utm_hello"].assert.isEqualTo("hello")
    params["sdkType"].assert.isEqualTo("Unreal")
    params["sdkVersion"].assert.isEqualTo("1.0.0")
  }

  @Test
  fun `doesn't create organization when invitation provided`() {
    val base = dbPopulator.createBase()
    val project = base.project
    val invitation =
      invitationService.create(
        CreateProjectInvitationParams(project, ProjectPermissionType.EDIT),
      )
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "aaaa@aaaa.com",
        invitationCode = invitation.code,
      )
    performPost("/api/public/sign_up", dto).andIsOk
    assertThat(organizationRepository.findAllByName("Pavel Novak")).hasSize(0)
  }

  @Test
  fun `doesn't create orgs when disabled`() {
    tolgeeProperties.authentication.userCanCreateOrganizations = false
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "aaaa@aaaa.com",
        organizationName = "Jejda",
      )
    performPost("/api/public/sign_up", dto).andIsOk
    assertThat(organizationRepository.findAllByName("Jejda")).hasSize(0)
  }

  @Test
  fun `doesn't allow sign up when disabled`() {
    tolgeeProperties.authentication.registrationsAllowed = false
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "aaaa@aaaa.com",
        organizationName = "Jejda",
      )
    performPost("/api/public/sign_up", dto).andIsUnauthorized
  }

  @Test
  fun `returns error when signing up with disabled account email`() {
    val dto =
      SignUpDto(
        name = "Pavel Novak",
        password = "aaaaaaaaa",
        email = "disabled@test.com",
      )
    performPost("/api/public/sign_up", dto).andIsOk

    val user = userAccountService.findActive("disabled@test.com")!!
    userAccountService.disable(user.id)

    val dto2 =
      SignUpDto(
        name = "Another User",
        password = "bbbbbbbbb",
        email = "disabled@test.com",
      )
    performPost("/api/public/sign_up", dto2)
      .andIsBadRequest
      .andAssertError
      .hasCode(Message.USER_ACCOUNT_DISABLED.code)
  }

  @Test
  fun `returns error when logging in with disabled account`() {
    val dto =
      SignUpDto(
        name = "Login Disabled",
        password = "aaaaaaaaa",
        email = "login-disabled@test.com",
      )
    performPost("/api/public/sign_up", dto).andIsOk

    val user = userAccountService.findActive("login-disabled@test.com")!!
    userAccountService.disable(user.id)

    doAuthentication("login-disabled@test.com", "aaaaaaaaa")
      .andIsUnauthorized
      .andAssertError
      .hasCode(Message.USER_ACCOUNT_DISABLED.code)
  }

  @Test
  fun testSignUpValidationBlankEmail() {
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaa", email = "")
    performPost("/api/public/sign_up", dto)
      .andIsBadRequest
      .andAssertResponse
      .error()
      .isStandardValidation
      .onField("email")
  }

  @Test
  fun testSignUpValidationBlankName() {
    val dto = SignUpDto(name = "", password = "aaaa", email = "aaa@aaa.cz")
    performPost("/api/public/sign_up", dto)
      .andIsBadRequest
      .andAssertResponse
      .error()
      .isStandardValidation
      .onField("name")
  }

  @Test
  fun testSignUpValidationInvalidEmail() {
    val dto = SignUpDto(name = "", password = "aaaa", email = "aaaaaa.cz")
    performPost("/api/public/sign_up", dto)
      .andIsBadRequest
      .andAssertResponse
      .error()
      .isStandardValidation
      .onField("email")
  }

  @Test
  fun `rejects sign up from a disposable email domain`() {
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "spammer@mailinator.com")
    performPost("/api/public/sign_up", dto)
      .andIsBadRequest
      .andHasErrorMessage(Message.EMAIL_DOMAIN_NOT_ALLOWED)
  }

  @Test
  fun `allows a disposable domain when disposable blocking is disabled`() {
    tolgeeProperties.authentication.blockDisposableEmails = false
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "spammer@mailinator.com")
    performPost("/api/public/sign_up", dto).andIsOk
  }

  @Test
  fun `rejects sign up from an admin-configured blocked domain`() {
    tolgeeProperties.authentication.blockedEmailDomains.add("blocked.example")
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "someone@blocked.example")
    performPost("/api/public/sign_up", dto)
      .andIsBadRequest
      .andHasErrorMessage(Message.EMAIL_DOMAIN_NOT_ALLOWED)
  }

  @Test
  fun `allows a domain that is whitelisted even when also disposable`() {
    tolgeeProperties.authentication.allowedEmailDomains.add("mailinator.com")
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "spammer@mailinator.com")
    performPost("/api/public/sign_up", dto).andIsOk
  }

  @Test
  fun `rejects a plus-alias of an existing account`() {
    dbPopulator.createUserIfNotExists("foo@gmail.com")
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "foo+spam@gmail.com")
    performPost("/api/public/sign_up", dto)
      .andIsBadRequest
      .andHasErrorMessage(Message.USERNAME_ALREADY_EXISTS)
  }

  @Test
  fun `rejects a case variant of an existing account`() {
    dbPopulator.createUserIfNotExists("foo@gmail.com")
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "FOO@gmail.com")
    performPost("/api/public/sign_up", dto)
      .andIsBadRequest
      .andHasErrorMessage(Message.USERNAME_ALREADY_EXISTS)
  }

  @Test
  fun `allows a distinct email on the same domain as an existing account`() {
    dbPopulator.createUserIfNotExists("foo@gmail.com")
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "bar@gmail.com")
    performPost("/api/public/sign_up", dto).andIsOk
  }

  @Test
  fun `allows a plus-alias when alias blocking is disabled`() {
    tolgeeProperties.authentication.blockEmailAliases = false
    dbPopulator.createUserIfNotExists("foo@gmail.com")
    val dto = SignUpDto(name = "Pavel Novak", password = "aaaaaaaaa", email = "foo+spam@gmail.com")
    performPost("/api/public/sign_up", dto).andIsOk
  }
}
