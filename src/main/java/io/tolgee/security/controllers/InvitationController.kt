package io.tolgee.security.controllers

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
class InvitationController @Autowired constructor(
        private val invitationService: InvitationService,
        private val securityService: SecurityService,
        private val repositoryService: RepositoryService
) {
    @GetMapping("/accept/{code}")
    fun acceptInvitation(@PathVariable("code") code: String?) {
        invitationService.removeExpired()
        invitationService.accept(code)
    }

    @GetMapping("/list/{repositoryId}")
    fun getRepositoryInvitations(@PathVariable("repositoryId") id: Long): Set<InvitationDTO> {
        val repository = repositoryService.findById(id).orElseThrow { NotFoundException() }
        securityService.checkRepositoryPermission(id, Permission.RepositoryPermissionType.MANAGE)
        return invitationService.getForRepository(repository).stream().map { invitation: Invitation? ->
            InvitationDTO.fromEntity(invitation)
        }.collect(Collectors.toCollection { LinkedHashSet() })
    }

    @DeleteMapping("/{invitationId}")
    fun deleteInvitation(@PathVariable("invitationId") id: Long?) {
        val invitation = invitationService.findById(id).orElseThrow {
            NotFoundException()
        }
        securityService.checkRepositoryPermission(invitation.permission!!.repository!!.id,
                Permission.RepositoryPermissionType.MANAGE)
        invitationService.delete(invitation)
    }
}