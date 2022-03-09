package io.tolgee.security.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.V2InvitationController
import io.tolgee.dtos.response.InvitationDTO
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.service.InvitationService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/invitation")
@Tag(name = "User invitations to project")
class InvitationController @Autowired constructor(
  private val invitationService: InvitationService,
  private val securityService: SecurityService,
  private val projectService: ProjectService,
  private val v2InvitationController: V2InvitationController
) {
  @GetMapping("/accept/{code}")
  @Operation(summary = "Accepts invitation to project")
  fun acceptInvitation(@PathVariable("code") code: String?): ResponseEntity<Void> {
    invitationService.removeExpired()
    invitationService.accept(code)
    return ResponseEntity(HttpStatus.OK)
  }

  @GetMapping("/list/{projectId}")
  @Operation(summary = "Prints all invitations to project")
  fun getProjectInvitations(@PathVariable("projectId") id: Long): Set<InvitationDTO> {
    val repository = projectService.get(id)
    securityService.checkProjectPermission(id, Permission.ProjectPermissionType.MANAGE)
    return invitationService.getForProject(repository).stream().map { invitation: Invitation? ->
      InvitationDTO.fromEntity(invitation)
    }.collect(Collectors.toCollection { LinkedHashSet() })
  }

  @DeleteMapping("/{invitationId}")
  @Operation(summary = "Deletes invitation by ID")
  fun deleteInvitation(@PathVariable("invitationId") id: Long): ResponseEntity<Void> {
    return v2InvitationController.deleteInvitation(id)
  }
}
