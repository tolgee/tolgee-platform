package io.tolgee.api.v2.controllers.organizationController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.config.TestEmailConfiguration
import io.tolgee.dtos.misc.CreateOrganizationInvitationParams
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.dtos.request.organization.OrganizationInviteUserDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andGetContentAsString
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.satisfies
import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration::class)
class OrganizationControllerInvitingTest : AuthorizedControllerTest() {
  companion object {
    private const val INVITED_EMAIL = "jon@doe.com"
    private const val INVITED_NAME = "Franta"
    private const val TEST_USERNAME = "hi@hi.com"
  }

  lateinit var dummyDto: OrganizationDto

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @BeforeEach
  fun setup() {
    dummyDto =
      OrganizationDto(
        "Test org",
        "This is description",
        "test-org",
      )
    emailTestUtil.initMocks()
  }

  @Test
  fun testGetAllInvitations() {
    val helloUser = dbPopulator.createUserIfNotExists("hellouser")

    this.organizationService.create(dummyDto, helloUser).let { organization ->
      val invitation =
        invitationService.create(
          CreateOrganizationInvitationParams(
            organization = organization,
            type = OrganizationRoleType.MEMBER,
          ),
        )
      loginAsUser("hellouser")
      performAuthGet("/v2/organizations/${organization.id}/invitations")
        .andIsOk
        .andAssertThatJson {
          node("_embedded.organizationInvitations").let { projectsNode ->
            projectsNode.isArray.hasSize(1)
            projectsNode.node("[0].id").isEqualTo(invitation.id)
          }
        }
    }
  }

  @Test
  fun testInviteUser() {
    val helloUser = dbPopulator.createUserIfNotExists("hellouser")
    loginAsUser(helloUser.username)

    this.organizationService.create(dummyDto, helloUser).let { organization ->
      val body = OrganizationInviteUserDto(roleType = OrganizationRoleType.MEMBER)
      performAuthPut("/v2/organizations/${organization.id}/invite", body).andPrettyPrint.andAssertThatJson {
        node("code").isString.hasSize(50).satisfies {
          invitationService.getInvitation(it) // it throws on not found
        }
        node("type").isEqualTo("MEMBER")
      }
    }
  }

  @Test
  fun testAcceptInvitation() {
    val helloUser = dbPopulator.createUserIfNotExists("hellouser")

    this.organizationService.create(dummyDto, helloUser).let { organization ->
      val invitation =
        invitationService.create(
          CreateOrganizationInvitationParams(
            organization = organization,
            type = OrganizationRoleType.OWNER,
          ),
        )
      val invitedUser = dbPopulator.createUserIfNotExists("invitedUser")
      loginAsUser(invitedUser.username)
      performAuthGet("/v2/invitations/${invitation.code}/accept").andIsOk
      assertThatThrownBy { invitationService.getInvitation(invitation.code) }
        .isInstanceOf(BadRequestException::class.java)
      organizationRoleService.hasAnyOrganizationRole(invitedUser.id, organization.id).let {
        assertThat(it).isTrue
      }
    }
  }

  @Test
  fun `it prevents accepting invitation again already a member`() {
    val helloUser = dbPopulator.createUserIfNotExists("hellouser")

    this.organizationService.create(dummyDto, helloUser).let { organization ->
      val invitation =
        invitationService.create(
          CreateOrganizationInvitationParams(
            organization = organization,
            type = OrganizationRoleType.MEMBER,
          ),
        )
      val invitedUser = dbPopulator.createUserIfNotExists("invitedUser")
      this.organizationRoleService.grantMemberRoleToUser(invitedUser, organization)
      loginAsUser(invitedUser.username)
      performAuthGet("/v2/invitations/${invitation.code}/accept").andIsBadRequest
    }
  }

  @Test
  fun testDeleteInvitation() {
    val organization = prepareTestOrganization()

    val invitation =
      invitationService.create(
        CreateOrganizationInvitationParams(
          organization = organization,
          type = OrganizationRoleType.MEMBER,
        ),
      )
    performAuthDelete("/v2/invitations/${invitation.id!!}", null).andIsOk
    assertThatThrownBy { invitationService.getInvitation(invitation.code) }
      .isInstanceOf(BadRequestException::class.java)
  }

  private fun prepareTestOrganization(): Organization {
    val helloUser = dbPopulator.createUserIfNotExists(TEST_USERNAME)
    val organization = organizationService.create(dummyDto, helloUser)
    loginAsUser(helloUser.username)
    return organization
  }

  @Test
  fun `stores name and e-mail with invitation`() {
    val organization = prepareTestOrganization()

    val code = inviteWithUserWithNameAndEmail(organization.id)
    val invitation = invitationService.getInvitation(code)
    assertThat(invitation.name).isEqualTo(INVITED_NAME)
    assertThat(invitation.email).isEqualTo(INVITED_EMAIL)
  }

  @Test
  fun `sends invitation e-mail`() {
    val organization = prepareTestOrganization()

    val code = inviteWithUserWithNameAndEmail(organization.id)
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      emailTestUtil.verifyEmailSent()
    }

    val messageContent = emailTestUtil.messageContents.single()
    assertThat(messageContent).contains(code)
    assertThat(messageContent).contains("https://dummy-url.com")
    emailTestUtil.assertEmailTo.isEqualTo(INVITED_EMAIL)
  }

  @Test
  fun `e-mail is sanitized`() {
    dummyDto.name = "Test org <a href='https://evil.local'>test</a>"
    val organization = prepareTestOrganization()

    inviteWithUserWithNameAndEmail(organization.id)
    waitForNotThrowing(timeout = 2000, pollTime = 25) {
      emailTestUtil.verifyEmailSent()
    }

    val messageContent = emailTestUtil.messageContents.single()
    assertThat(messageContent).doesNotContain("<a href='https://evil.local")
    assertThat(messageContent).contains("&lt;a href=&#39;https://evil.local")
  }

  @Test
  fun `does not invite when email already invited`() {
    val organization = prepareTestOrganization()
    performCreateInvitation(organization.id).andIsOk
    performCreateInvitation(organization.id).andIsBadRequest
  }

  @Test
  fun `does not invite when email already member`() {
    val organization = prepareTestOrganization()
    performAuthPut(
      "/v2/organizations/${organization.id}/invite",
      OrganizationInviteUserDto(
        roleType = OrganizationRoleType.MEMBER,
        email = TEST_USERNAME,
        name = INVITED_NAME,
      ),
    ).andIsBadRequest
  }

  private fun inviteWithUserWithNameAndEmail(organizationId: Long): String {
    val invitationJson = performCreateInvitation(organizationId).andIsOk.andGetContentAsString

    return jacksonObjectMapper().readValue<Map<String, Any>>(invitationJson)["code"] as String
  }

  private fun performCreateInvitation(organizationId: Long) =
    performAuthPut(
      "/v2/organizations/$organizationId/invite",
      OrganizationInviteUserDto(
        roleType = OrganizationRoleType.MEMBER,
        email = INVITED_EMAIL,
        name = INVITED_NAME,
      ),
    )
}
