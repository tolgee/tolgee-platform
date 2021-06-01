package io.tolgee.security.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.response.InvitationDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.service.InvitationService
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/invitation")
@Tag(name = "User invitations to project")
open class InvitationController @Autowired constructor(
        private val invitationService: InvitationService,
        private val securityService: SecurityService,
        private val projectService: ProjectService,
        private val organizationRoleService: OrganizationRoleService
) {
    @GetMapping("/accept/{code}")
    @Operation(summary = "Accepts invitation to project")
    open fun acceptInvitation(@PathVariable("code") code: String?): ResponseEntity<Void> {
        invitationService.removeExpired()
        invitationService.accept(code)
        return ResponseEntity(HttpStatus.OK)
    }

    @GetMapping("/list/{projectId}")
    @Operation(summary = "Prints all invitations to project")
    open fun getRepositoryInvitations(@PathVariable("projectId") id: Long): Set<InvitationDTO> {
        val repository = projectService.get(id).orElseThrow { NotFoundException() }!!
        securityService.checkRepositoryPermission(id, Permission.ProjectPermissionType.MANAGE)
        return invitationService.getForRepository(repository).stream().map { invitation: Invitation? ->
            InvitationDTO.fromEntity(invitation)
        }.collect(Collectors.toCollection { LinkedHashSet() })
    }

    @DeleteMapping("/{invitationId}")
    @Operation(summary = "Deletes invitation by ID")
    open fun deleteInvitation(@PathVariable("invitationId") id: Long): ResponseEntity<Void> {
        val invitation = invitationService.findById(id).orElseThrow {
            NotFoundException()
        }
        invitation.permission?.let {
            securityService.checkRepositoryPermission(invitation.permission!!.project!!.id,
                    Permission.ProjectPermissionType.MANAGE)
        }

        invitation.organizationRole?.let {
            organizationRoleService.checkUserIsOwner(invitation.organizationRole!!.organization!!.id!!)
        }

        invitationService.delete(invitation)
        return ResponseEntity(HttpStatus.OK)
    }
}
