package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.request.InviteUserDto
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.InvalidStateException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.InvitationService
import io.tolgee.service.RepositoryService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController("_repositoryController")
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/repositories")
@Tag(name = "Repository")
class RepositoryController @Autowired constructor(private val repositoryService: RepositoryService, private val authenticationFacade: AuthenticationFacade, private val securityService: SecurityService, private val invitationService: InvitationService) : IController {

    @PostMapping(value = [""])
    @Operation(summary = "Creates repository with specified languages")
    fun createRepository(@RequestBody @Valid dto: CreateRepositoryDTO?): RepositoryDTO {
        val userAccount = authenticationFacade.userAccount
        val repository = repositoryService.createRepository(dto!!, userAccount)
        return fromEntityAndPermission(repository, repository.permissions.stream().findAny().orElseThrow { InvalidStateException() })
    }

    @GetMapping(value = ["/{id}"])
    @Operation(summary = "Returns repository by id")
    fun getRepository(@PathVariable("id") id: Long?): RepositoryDTO {
        val permission = securityService.getAnyRepositoryPermissionOrThrow(id)
        return fromEntityAndPermission(repositoryService.getById(id!!).orElseThrow<RuntimeException>(null), permission)
    }

    @Operation(summary = "Modifies repository")
    @PostMapping(value = ["/edit"])
    fun editRepository(@RequestBody @Valid dto: EditRepositoryDTO?): RepositoryDTO {
        val permission = securityService.checkRepositoryPermission(dto!!.repositoryId, Permission.RepositoryPermissionType.MANAGE)
        val repository = repositoryService.editRepository(dto)
        return fromEntityAndPermission(repository, permission)
    }

    @GetMapping(value = [""])
    @Operation(summary = "Return all repositories, where use has any access")
    fun getAll(): Set<RepositoryDTO> = repositoryService.findAllPermitted(authenticationFacade.userAccount)

    @DeleteMapping(value = ["/{id}"])
    @Operation(summary = "Deletes repository by id")
    fun deleteRepository(@PathVariable id: Long?) {
        securityService.checkRepositoryPermission(id, Permission.RepositoryPermissionType.MANAGE)
        repositoryService.deleteRepository(id!!)
    }

    @PostMapping("/invite")
    @Operation(summary = "Generates user invitation link for repository")
    fun inviteUser(@RequestBody invitation: InviteUserDto): String {
        securityService.checkRepositoryPermission(invitation.repositoryId, Permission.RepositoryPermissionType.MANAGE)
        val repository = repositoryService.getById(invitation.repositoryId!!).orElseThrow { NotFoundException() }
        return invitationService.create(repository, invitation.type)
    }
}
