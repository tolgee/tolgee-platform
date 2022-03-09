package io.tolgee.api.v2.controllers

import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.fixtures.JavaMailSenderMocked
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mail.javamail.JavaMailSender
import javax.mail.internet.MimeMessage

class V2InvitationControllerTest : AuthorizedControllerTest(), JavaMailSenderMocked {

  @BeforeEach
  fun setup() {
    loginAsUser(initialUsername)
  }

  @MockBean
  @Autowired
  override lateinit var javaMailSender: JavaMailSender

  override lateinit var messageArgumentCaptor: ArgumentCaptor<MimeMessage>

  @Test
  fun `does not return when has no project permission`() {
    val project = dbPopulator.createBase(generateUniqueString())
    userAccount = dbPopulator.createUserIfNotExists("pavol")
    performAuthGet("/v2/projects/${project.id}/invitations").andIsForbidden
  }

  @Test
  fun `deletes translate invitation with languages`() {
    val project = dbPopulator.createBase(generateUniqueString())
    tolgeeProperties.frontEndUrl = "https://dummyUrl.com"
    val invitation = createTranslateInvitation(project)

    performAuthDelete("/v2/invitations/${invitation.id}").andIsOk
    assertThatThrownBy { invitationService.getInvitation(invitation.code) }
    assertThat(languageService.findAll(project.id)).hasSize(2)
  }

  @Test
  fun `deletes edit invitation`() {
    val project = dbPopulator.createBase(generateUniqueString())

    val invitation = invitationService.create(
      CreateProjectInvitationParams(
        project,
        Permission.ProjectPermissionType.EDIT
      )
    )

    performAuthDelete("/v2/invitations/${invitation.id}").andIsOk
    assertThatThrownBy { invitationService.getInvitation(invitation.code) }
  }

  @Test
  fun `accepts invitation`() {
    val project = dbPopulator.createBase(generateUniqueString())
    val code = invitationService.create(
      CreateProjectInvitationParams(
        project,
        Permission.ProjectPermissionType.EDIT
      )
    ).code

    val newUser = dbPopulator.createUserIfNotExists(generateUniqueString(), "pwd")
    loginAsUser(newUser.username)
    performAuthGet("/v2/invitations/$code/accept").andIsOk

    assertInvitationAccepted(project, newUser, Permission.ProjectPermissionType.EDIT)
  }

  @Test
  fun `accepts translate invitation with languages`() {
    val project = dbPopulator.createBase(generateUniqueString())
    val code = invitationService.create(
      CreateProjectInvitationParams(
        project,
        Permission.ProjectPermissionType.TRANSLATE,
        project.languages.toList()
      )
    ).code
    val newUser = dbPopulator.createUserIfNotExists(generateUniqueString(), "pwd")
    loginAsUser(newUser.username)

    performAuthGet("/v2/invitations/$code/accept").andIsOk
    assertInvitationAccepted(project, newUser, Permission.ProjectPermissionType.TRANSLATE)
  }

  private fun assertInvitationAccepted(
    project: Project,
    newUser: UserAccount,
    expectedType: Permission.ProjectPermissionType
  ) {
    assertThat(invitationService.getForProject(project)).hasSize(0)
    assertThat(permissionService.getProjectPermissionType(project.id, newUser)).isNotNull
    val type = permissionService.getProjectPermissionType(project.id, newUser)!!
    assertThat(type).isEqualTo(expectedType)
  }

  private fun createTranslateInvitation(project: Project): Invitation {
    return invitationService.create(
      CreateProjectInvitationParams(
        project = project,
        type = Permission.ProjectPermissionType.TRANSLATE,
        languages = project.languages.toList(),
        name = "Franta",
        email = "a@a.a"
      )
    )
  }
}
