package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.request.RepositoryInviteUserDto
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.InvitationService
import io.tolgee.service.PermissionService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController("_projectController")
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/repositories")
@Tag(name = "Repository")
open class RepositoryController @Autowired constructor(private val projectService: ProjectService,
                                                       private val authenticationFacade: AuthenticationFacade,
                                                       private val securityService: SecurityService,
                                                       private val invitationService: InvitationService,
                                                       private val permissionService: PermissionService,
                                                       private val tolgeeProperties: TolgeeProperties
) : IController {

    @PostMapping(value = [""])
    @Operation(summary = "Creates project with specified languages")
    open fun createRepository(@RequestBody @Valid dto: CreateRepositoryDTO?): RepositoryDTO {
        val userAccount = authenticationFacade.userAccount
        if (!this.tolgeeProperties.authentication.userCanCreateRepositories
                && userAccount.role != UserAccount.Role.ADMIN) {
            throw PermissionException()
        }
        val project = projectService.createRepository(dto!!)
        val type = permissionService.getRepositoryPermissionType(project.id, userAccount)
                ?: throw IllegalStateException()
        return fromEntityAndPermission(project, type)
    }

    @GetMapping(value = ["/{id}"])
    @Operation(summary = "Returns project by id")
    open fun getRepository(@PathVariable("id") id: Long?): RepositoryDTO {
        val permission = securityService.checkAnyRepositoryPermission(id!!)
        val project = projectService.get(id).orElseThrow { NotFoundException() }!!
        return fromEntityAndPermission(project, permission)
    }

    @Operation(summary = "Modifies project")
    @PostMapping(value = ["/edit"])
    open fun editRepository(@RequestBody @Valid dto: EditRepositoryDTO?): RepositoryDTO {
        val permission = securityService.checkRepositoryPermission(dto!!.projectId!!, Permission.ProjectPermissionType.MANAGE)
        val project = projectService.editRepository(dto)
        return fromEntityAndPermission(project, permission)
    }

    @GetMapping(value = [""])
    @Operation(summary = "Return all repositories, where use has any access")
    open fun getAll(): List<RepositoryDTO> = projectService.findAllPermitted(authenticationFacade.userAccount)

    @DeleteMapping(value = ["/{id}"])
    @Operation(summary = "Deletes project by id")
    open fun deleteRepository(@PathVariable id: Long?) {
        securityService.checkRepositoryPermission(id!!, Permission.ProjectPermissionType.MANAGE)
        projectService.deleteRepository(id)
    }

    @PostMapping("/invite")
    @Operation(summary = "Generates user invitation link for project")
    open fun inviteUser(@RequestBody @Valid invitation: RepositoryInviteUserDto): String {
        securityService.checkRepositoryPermission(invitation.projectId!!, Permission.ProjectPermissionType.MANAGE)
        val project = projectService.get(invitation.projectId!!).orElseThrow { NotFoundException() }!!
        return invitationService.create(project, invitation.type!!)
    }
}
