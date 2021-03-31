package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.request.InviteUserDto
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
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
@Tag(name = "Repository")
open class RepositoryController @Autowired constructor(private val repositoryService: RepositoryService,
                                                  private val authenticationFacade: AuthenticationFacade,
                                                  private val securityService: SecurityService,
                                                  private val invitationService: InvitationService,
                                                  private val permissionService: PermissionService,
                                                  private val tolgeeProperties: TolgeeProperties
) : IController {

    @PostMapping(value = [""])
    @Operation(summary = "Creates repository with specified languages")
    open fun createRepository(@RequestBody @Valid dto: CreateRepositoryDTO?): RepositoryDTO {
        val userAccount = authenticationFacade.userAccount
        if (!this.tolgeeProperties.authentication.userCanCreateRepositories
                && userAccount.role != UserAccount.Role.ADMIN) {
            throw PermissionException()
        }
        val repository = repositoryService.createRepository(dto!!)
        val type = permissionService.getRepositoryPermissionType(repository.id, userAccount)
                ?: throw IllegalStateException()
        return fromEntityAndPermission(repository, type)
    }

    @GetMapping(value = ["/{id}"])
    @Operation(summary = "Returns repository by id")
    open fun getRepository(@PathVariable("id") id: Long?): RepositoryDTO {
        val permission = securityService.checkAnyRepositoryPermission(id!!)
        val repository = repositoryService.get(id).orElseThrow { NotFoundException() }!!
        return fromEntityAndPermission(repository, permission)
    }

    @Operation(summary = "Modifies repository")
    @PostMapping(value = ["/edit"])
    open fun editRepository(@RequestBody @Valid dto: EditRepositoryDTO?): RepositoryDTO {
        val permission = securityService.checkRepositoryPermission(dto!!.repositoryId!!, Permission.RepositoryPermissionType.MANAGE)
        val repository = repositoryService.editRepository(dto)
        return fromEntityAndPermission(repository, permission)
    }

    @GetMapping(value = [""])
    @Operation(summary = "Return all repositories, where use has any access")
    open fun getAll(): List<RepositoryDTO> = repositoryService.findAllPermitted(authenticationFacade.userAccount)

    @DeleteMapping(value = ["/{id}"])
    @Operation(summary = "Deletes repository by id")
    open fun deleteRepository(@PathVariable id: Long?) {
        securityService.checkRepositoryPermission(id!!, Permission.RepositoryPermissionType.MANAGE)
        repositoryService.deleteRepository(id)
    }

    @PostMapping("/invite")
    @Operation(summary = "Generates user invitation link for repository")
    open fun inviteUser(@RequestBody invitation: InviteUserDto): String {
        securityService.checkRepositoryPermission(invitation.repositoryId!!, Permission.RepositoryPermissionType.MANAGE)
        val repository = repositoryService.get(invitation.repositoryId!!).orElseThrow { NotFoundException() }
        return invitationService.create(repository, invitation.type)
    }
}
