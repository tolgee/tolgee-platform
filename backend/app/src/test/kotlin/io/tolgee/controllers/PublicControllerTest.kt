package io.tolgee.controllers

import com.posthog.server.PostHog
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.fixtures.andAssertResponse
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.properties.Delegates

@AutoConfigureMockMvc
class PublicControllerTest : AbstractControllerTest() {
  private var canCreateOrganizations by Delegates.notNull<Boolean>()
  private var registrationsAllowed by Delegates.notNull<Boolean>()

  @BeforeEach
  fun setup() {
    Mockito.reset(postHog)
    canCreateOrganizations = tolgeeProperties.authentication.userCanCreateOrganizations
    registrationsAllowed = tolgeeProperties.authentication.registrationsAllowed
  }

  @AfterEach
  fun tearDown() {
    tolgeeProperties.authentication.userCanCreateOrganizations = canCreateOrganizations
    tolgeeProperties.authentication.registrationsAllowed = registrationsAllowed
  }

  @MockitoBean
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
}
