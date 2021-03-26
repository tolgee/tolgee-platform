package io.tolgee.security.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.response.InvitationDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.service.InvitationService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/invitation")
@Tag(name = "User invitations to repository")
class InvitationController @Autowired constructor(
        private val invitationService: InvitationService,
        private val securityService: SecurityService,
        private val repositoryService: RepositoryService
) {
    @GetMapping("/accept/{code}")
    @Operation(summary = "Accepts invitation to repository")
    fun acceptInvitation(@PathVariable("code") code: String?) {
        invitationService.removeExpired()
        invitationService.accept(code)
    }

    @GetMapping("/list/{repositoryId}")
    @Operation(summary = "Prints all invitations to repository")
    fun getRepositoryInvitations(@PathVariable("repositoryId") id: Long): Set<InvitationDTO> {
        val repository = repositoryService.getById(id).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(id, Permission.RepositoryPermissionType.MANAGE)
        return invitationService.getForRepository(repository).stream().map { invitation: Invitation? ->
            InvitationDTO.fromEntity(invitation)
        }.collect(Collectors.toCollection { LinkedHashSet() })
    }

    @DeleteMapping("/{invitationId}")
    @Operation(summary = "Deletes invitation by ID")
    fun deleteInvitation(@PathVariable("invitationId") id: Long?) {
        val invitation = invitationService.findById(id).orElseThrow {
            NotFoundException()
        }
        securityService.checkRepositoryPermission(invitation.permission!!.repository!!.id,
                Permission.RepositoryPermissionType.MANAGE)
        invitationService.delete(invitation)
    }
}
