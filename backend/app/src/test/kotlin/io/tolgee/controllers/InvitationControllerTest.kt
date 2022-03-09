package io.tolgee.controllers

import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.response.InvitationDTO
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.model.Permission
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class InvitationControllerTest : AuthorizedControllerTest() {

  @BeforeEach
  fun login() {
    loginAsUser(initialUsername)
  }

  @Test
  fun getProjectInvitations() {
    val project = dbPopulator.createBase(generateUniqueString())
    val invitation = invitationService.create(
      CreateProjectInvitationParams(
        project,
        Permission.ProjectPermissionType.MANAGE
      )
    )
    val response = performAuthGet("/api/invitation/list/${project.id}").andExpect(status().isOk).andReturn()
    val list: List<InvitationDTO> = response.mapResponseTo()
    assertThat(list).hasSize(1)
    assertThat(list[0].code).isEqualTo(invitation.code)
  }

  @Test
  fun acceptInvitation() {
    val project = dbPopulator.createBase(generateUniqueString())
    val invitation = invitationService.create(
      CreateProjectInvitationParams(project, Permission.ProjectPermissionType.EDIT)
    )

    val newUser = dbPopulator.createUserIfNotExists(generateUniqueString(), "pwd")
    loginAsUser(newUser.username)
    performAuthGet("/api/invitation/accept/${invitation.code}").andExpect(status().isOk).andReturn()

    assertThat(invitationService.getForProject(project)).hasSize(0)
    assertThat(permissionService.getProjectPermissionType(project.id, newUser)).isNotNull
    val type = permissionService.getProjectPermissionType(project.id, newUser)!!
    assertThat(type).isEqualTo(Permission.ProjectPermissionType.EDIT)
  }
}
