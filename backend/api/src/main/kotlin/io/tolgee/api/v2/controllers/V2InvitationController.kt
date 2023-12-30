package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.service.InvitationService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.security.SecurityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/invitations")
@Tag(name = "User invitations to project or organization")
class V2InvitationController(
  private val invitationService: InvitationService,
  private val securityService: SecurityService,
  private val organizationRoleService: OrganizationRoleService,
) {
  @GetMapping("/{code}/accept")
  @Operation(summary = "Accepts invitation to project or organization")
  fun acceptInvitation(
    @PathVariable("code") code: String?,
  ): ResponseEntity<Void> {
    invitationService.accept(code)
    return ResponseEntity(HttpStatus.OK)
  }

  @DeleteMapping("/{invitationId}")
  @Operation(summary = "Deletes invitation by ID")
  fun deleteInvitation(
    @PathVariable("invitationId") id: Long,
  ): ResponseEntity<Void> {
    val invitation =
      invitationService.findById(id).orElseThrow {
        NotFoundException()
      }
    invitation.permission?.let {
      securityService.checkProjectPermission(
        invitation.permission!!.project!!.id,
        Scope.ADMIN,
      )
    }

    invitation.organizationRole?.let {
      organizationRoleService.checkUserIsOwner(invitation.organizationRole!!.organization!!.id)
    }

    invitationService.delete(invitation)
    return ResponseEntity(HttpStatus.OK)
  }
}
