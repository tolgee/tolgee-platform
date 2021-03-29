package io.tolgee.controllers

import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.request.InviteUser
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.InvitationService
import io.tolgee.service.PermissionService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController("_repositoryController")
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/repositories")
class RepositoryController @Autowired constructor(private val repositoryService: RepositoryService,
                                                  private val authenticationFacade: AuthenticationFacade,
                                                  private val securityService: SecurityService,
                                                  private val invitationService: InvitationService,
                                                  private val permissionService: PermissionService
) : IController {
    @PostMapping(value = [""])
    fun createRepository(@RequestBody @Valid dto: CreateRepositoryDTO?): RepositoryDTO {
        val repository = repositoryService.createRepository(dto!!)
        val permission = permissionService.getRepositoryPermissionType(repository.id, authenticationFacade.userAccount)
                ?: throw IllegalStateException()
        return fromEntityAndPermission(repository, permission)
    }

    @GetMapping(value = ["/{id}"])
    fun getRepository(@PathVariable("id") id: Long?): RepositoryDTO {
        val permission = securityService.checkAnyRepositoryPermission(id!!)
        return fromEntityAndPermission(repositoryService.get(id)
                .orElseThrow<RuntimeException>(null), permission)
    }

    @PostMapping(value = ["/edit"])
    fun editRepository(@RequestBody @Valid dto: EditRepositoryDTO?): RepositoryDTO {
        val permission = securityService.checkRepositoryPermission(dto!!.repositoryId!!, Permission.RepositoryPermissionType.MANAGE)
        val repository = repositoryService.editRepository(dto)
        return fromEntityAndPermission(repository, permission)
    }

    @GetMapping(value = [""])
    fun getAll(): Set<RepositoryDTO> {
        return repositoryService.findAllPermitted(authenticationFacade.userAccount)
    }

    @DeleteMapping(value = ["/{id}"])
    fun deleteRepository(@PathVariable id: Long?) {
        securityService.checkRepositoryPermission(id!!, Permission.RepositoryPermissionType.MANAGE)
        repositoryService.deleteRepository(id)
    }

    @PostMapping("/invite")
    fun inviteUser(@RequestBody invitation: InviteUser): String {
        securityService.checkRepositoryPermission(invitation.repositoryId, Permission.RepositoryPermissionType.MANAGE)
        val repository = repositoryService.get(invitation.repositoryId).orElseThrow { NotFoundException() }
        return invitationService.create(repository, invitation.type)
    }
}
