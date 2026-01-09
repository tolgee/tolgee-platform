package io.tolgee.api.v2.controllers

import io.tolgee.config.TestEmailConfiguration
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.organization.OrganizationInviteUserDto
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.equalsPermissionType
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.hateoas.invitation.OrganizationInvitationModel
import io.tolgee.hateoas.invitation.ProjectInvitationModel
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(TestEmailConfiguration::class)
class V2InvitationControllerTest : AuthorizedControllerTest() {
  @BeforeEach
  fun setup() {
    loginAsUser(initialUsername)
    emailTestUtil.initMocks()
  }

  @Autowired
  private lateinit var emailTestUtil: EmailTestUtil

  @Test
  fun `does not return when has no project permission`() {
    val base = dbPopulator.createBase()
    val project = base.project
    userAccount = dbPopulator.createUserIfNotExists("pavol")
    performAuthGet("/v2/projects/${project.id}/invitations").andIsNotFound
  }

  @Test
  fun `deletes translate invitation with languages`() {
    val base = dbPopulator.createBase()
    val project = base.project
    val invitation = createTranslateInvitation(project)

    performAuthDelete("/v2/invitations/${invitation.id}").andIsOk
    assertThatThrownBy { invitationService.getInvitation(invitation.code) }
    assertThat(languageService.findAll(project.id)).hasSize(2)
  }

  @Test
  fun `project invitation contains creator info`() {
    val base = dbPopulator.createBase()
    val project = base.project
    val invitation = createTranslateInvitation(project)
    assertThat(invitation.createdBy?.name).isEqualTo("admin")
  }

  @Test
  fun `organization invitation contains creator info`() {
    val base = dbPopulator.createBase()
    val invitation = createOrganizationInvitation(base.organization)
    assertThat(invitation.createdBy?.name).isEqualTo("admin")
  }

  @Test
  fun `deletes invitations created by deleted user`() {
    val base = dbPopulator.createBase()
    val newUser = dbPopulator.createUserIfNotExists("manager")
    permissionService.grantFullAccessToProject(newUser, base.project)

    loginAsUser(newUser.username)
    val invitation = createTranslateInvitation(base.project)
    performAuthGet("/api/public/invitation_info/${invitation.code}")
      .andIsOk
      .andAssertThatJson {
        node("createdBy.name").isEqualTo(newUser.name)
      }
    performAuthDelete("/v2/user")
    loginAsUser(base.userAccount)
    performAuthGet("/api/public/invitation_info/${invitation.code}")
      .andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("invitation_code_does_not_exist_or_expired")
      }
  }

  @Test
  fun `deletes edit invitation`() {
    val base = dbPopulator.createBase()
    val project = base.project

    val invitation =
      invitationService.create(
        CreateProjectInvitationParams(
          project,
          ProjectPermissionType.EDIT,
        ),
      )

    performAuthDelete("/v2/invitations/${invitation.id}").andIsOk
    assertThatThrownBy { invitationService.getInvitation(invitation.code) }
  }

  @Test
  fun `accepts invitation`() {
    val base = dbPopulator.createBase()
    val project = base.project
    val code =
      invitationService
        .create(
          CreateProjectInvitationParams(
            project,
            ProjectPermissionType.EDIT,
          ),
        ).code

    val newUser = dbPopulator.createUserIfNotExists(generateUniqueString(), "pwd")
    loginAsUser(newUser.username)
    performAuthGet("/v2/invitations/$code/accept").andIsOk

    assertInvitationAccepted(project, newUser, ProjectPermissionType.EDIT)
  }

  @Test
  fun `accepts translate invitation with languages`() {
    val base = dbPopulator.createBase()
    val project = base.project
    val code =
      invitationService
        .create(
          CreateProjectInvitationParams(
            project,
            ProjectPermissionType.TRANSLATE,
            LanguagePermissions(translate = project.languages, null, null, null),
            null,
          ),
        ).code
    val newUser = dbPopulator.createUserIfNotExists(generateUniqueString(), "pwd")
    loginAsUser(newUser.username)

    performAuthGet("/v2/invitations/$code/accept").andIsOk
    assertInvitationAccepted(project, newUser, ProjectPermissionType.TRANSLATE)
  }

  private fun assertInvitationAccepted(
    project: Project,
    newUser: UserAccount,
    expectedType: ProjectPermissionType,
  ) {
    assertThat(invitationService.getForProject(project)).hasSize(0)
    assertThat(permissionService.getProjectPermissionScopesNoApiKey(project.id, newUser)).isNotNull
    val type = permissionService.getProjectPermissionScopesNoApiKey(project.id, newUser)!!
    type.assert.equalsPermissionType(expectedType)
  }

  private fun createTranslateInvitation(project: Project): ProjectInvitationModel {
    val result =
      performAuthPut(
        "/v2/projects/${project.id}/invite",
        ProjectInviteUserDto(
          ProjectPermissionType.TRANSLATE,
          translateLanguages = project.languages.map { it.id }.toSet(),
          name = "Franta",
          email = "a@a.a",
        ),
      ).andIsOk

    return mapper.readValue(result.andReturn().response.contentAsByteArray, ProjectInvitationModel::class.java)
  }

  private fun createOrganizationInvitation(organization: Organization): OrganizationInvitationModel {
    val result =
      performAuthPut(
        "/v2/organizations/${organization.id}/invite",
        OrganizationInviteUserDto(
          roleType = OrganizationRoleType.MEMBER,
          name = "Franta",
          email = "a@a.a",
        ),
      ).andIsOk

    return mapper.readValue(result.andReturn().response.contentAsByteArray, OrganizationInvitationModel::class.java)
  }
}
