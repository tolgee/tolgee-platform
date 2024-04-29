package io.tolgee.api.v2.controllers

import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.equalsPermissionType
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Invitation
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    userAccount = dbPopulator.createUserIfNotExists("pavol")
    performAuthGet("/v2/projects/${project.id}/invitations").andIsNotFound
  }

  @Test
  fun `deletes translate invitation with languages`() {
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    tolgeeProperties.frontEndUrl = "https://dummyUrl.com"
    val invitation = createTranslateInvitation(project)

    performAuthDelete("/v2/invitations/${invitation.id}").andIsOk
    assertThatThrownBy { invitationService.getInvitation(invitation.code) }
    assertThat(languageService.findAll(project.id)).hasSize(2)
  }

  @Test
  fun `deletes edit invitation`() {
    val base = dbPopulator.createBase(generateUniqueString())
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
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    val code =
      invitationService.create(
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
    val base = dbPopulator.createBase(generateUniqueString())
    val project = base.project
    val code =
      invitationService.create(
        CreateProjectInvitationParams(
          project,
          ProjectPermissionType.TRANSLATE,
          LanguagePermissions(translate = project.languages, null, null),
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

  private fun createTranslateInvitation(project: Project): Invitation {
    return invitationService.create(
      CreateProjectInvitationParams(
        project = project,
        type = ProjectPermissionType.TRANSLATE,
        languagePermissions = LanguagePermissions(translate = project.languages, view = null, stateChange = null),
        name = "Franta",
        email = "a@a.a",
        scopes = null,
      ),
    )
  }
}
