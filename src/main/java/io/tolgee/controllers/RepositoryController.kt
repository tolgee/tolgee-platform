package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.CreateProjectDTO
import io.tolgee.dtos.request.EditProjectDTO
import io.tolgee.dtos.request.ProjectInviteUserDto
import io.tolgee.dtos.response.ProjectDTO
import io.tolgee.dtos.response.ProjectDTO.Companion.fromEntityAndPermission
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
@RequestMapping("/api/projects")
@Tag(name = "Project")
open class ProjectController @Autowired constructor(private val projectService: ProjectService,
                                                       private val authenticationFacade: AuthenticationFacade,
                                                       private val securityService: SecurityService,
                                                       private val invitationService: InvitationService,
                                                       private val permissionService: PermissionService,
                                                       private val tolgeeProperties: TolgeeProperties
) : IController {

    @PostMapping(value = [""])
    @Operation(summary = "Creates project with specified languages")
    open fun createProject(@RequestBody @Valid dto: CreateProjectDTO?): ProjectDTO {
        val userAccount = authenticationFacade.userAccount
        if (!this.tolgeeProperties.authentication.userCanCreateProjects
                && userAccount.role != UserAccount.Role.ADMIN) {
            throw PermissionException()
        }
        val project = projectService.createProject(dto!!)
        val type = permissionService.getProjectPermissionType(project.id, userAccount)
                ?: throw IllegalStateException()
        return fromEntityAndPermission(project, type)
    }

    @GetMapping(value = ["/{id}"])
    @Operation(summary = "Returns project by id")
    open fun getProject(@PathVariable("id") id: Long?): ProjectDTO {
        val permission = securityService.checkAnyProjectPermission(id!!)
        val project = projectService.get(id).orElseThrow { NotFoundException() }!!
        return fromEntityAndPermission(project, permission)
    }

    @Operation(summary = "Modifies project")
    @PostMapping(value = ["/edit"])
    open fun editProject(@RequestBody @Valid dto: EditProjectDTO?): ProjectDTO {
        val permission = securityService.checkProjectPermission(dto!!.projectId!!, Permission.ProjectPermissionType.MANAGE)
        val project = projectService.editProject(dto)
        return fromEntityAndPermission(project, permission)
    }

    @GetMapping(value = [""])
    @Operation(summary = "Return all projects, where use has any access")
    open fun getAll(): List<ProjectDTO> = projectService.findAllPermitted(authenticationFacade.userAccount)

    @DeleteMapping(value = ["/{id}"])
    @Operation(summary = "Deletes project by id")
    open fun deleteProject(@PathVariable id: Long?) {
        securityService.checkProjectPermission(id!!, Permission.ProjectPermissionType.MANAGE)
        projectService.deleteProject(id)
    }

    @PostMapping("/invite")
    @Operation(summary = "Generates user invitation link for project")
    open fun inviteUser(@RequestBody @Valid invitation: ProjectInviteUserDto): String {
        securityService.checkProjectPermission(invitation.projectId!!, Permission.ProjectPermissionType.MANAGE)
        val project = projectService.get(invitation.projectId!!).orElseThrow { NotFoundException() }!!
        return invitationService.create(project, invitation.type!!)
    }
}
