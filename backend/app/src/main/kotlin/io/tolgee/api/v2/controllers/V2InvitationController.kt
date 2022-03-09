package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.service.InvitationService
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/")
@Tag(name = "User invitations to project")
class V2InvitationController @Autowired constructor(
  private val invitationService: InvitationService,
  private val securityService: SecurityService,
  private val organizationRoleService: OrganizationRoleService,
) {
  @GetMapping("invitations/{code}/accept")
  @Operation(summary = "Accepts invitation to project or organization")
  fun acceptInvitation(@PathVariable("code") code: String?): ResponseEntity<Void> {
    invitationService.removeExpired()
    invitationService.accept(code)
    return ResponseEntity(HttpStatus.OK)
  }

  @DeleteMapping("invitations/{invitationId}")
  @Operation(summary = "Deletes invitation by ID")
  fun deleteInvitation(@PathVariable("invitationId") id: Long): ResponseEntity<Void> {
    val invitation = invitationService.findById(id).orElseThrow {
      NotFoundException()
    }
    invitation.permission?.let {
      securityService.checkProjectPermission(
        invitation.permission!!.project.id,
        Permission.ProjectPermissionType.MANAGE
      )
    }

    invitation.organizationRole?.let {
      organizationRoleService.checkUserIsOwner(invitation.organizationRole!!.organization!!.id)
    }

    invitationService.delete(invitation)
    return ResponseEntity(HttpStatus.OK)
  }
}
