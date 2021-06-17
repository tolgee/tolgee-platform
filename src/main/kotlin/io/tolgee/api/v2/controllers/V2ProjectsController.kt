/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.project.ProjectModel
import io.tolgee.api.v2.hateoas.project.ProjectModelAssembler
import io.tolgee.api.v2.hateoas.user_account.UserAccountInProjectModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountInProjectModelAssembler
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.CreateProjectDTO
import io.tolgee.dtos.request.EditProjectDTO
import io.tolgee.dtos.request.ProjectInviteUserDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ProjectView
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.*
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Projects")
class V2ProjectsController(
        private val projectService: ProjectService,
        private val projectHolder: ProjectHolder,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val arrayResourcesAssembler: PagedResourcesAssembler<ProjectView>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val userArrayResourcesAssembler: PagedResourcesAssembler<UserAccountInProjectView>,
        private val userAccountInProjectModelAssembler: UserAccountInProjectModelAssembler,
        private val projectModelAssembler: ProjectModelAssembler,
        private val userAccountService: UserAccountService,
        private val permissionService: PermissionService,
        private val authenticationFacade: AuthenticationFacade,
        private val tolgeeProperties: TolgeeProperties,
        private val securityService: SecurityService,
        private val invitationService: InvitationService,
) {
    @Operation(summary = "Returns all projects, which are current user permitted to view")
    @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
    fun getAll(pageable: Pageable, @RequestParam("search") search: String?): PagedModel<ProjectModel>? {
        val projects = projectService.findPermittedPaged(pageable, search)
        return arrayResourcesAssembler.toModel(projects, projectModelAssembler)
    }

    @GetMapping("/{projectId}")
    @AccessWithAnyProjectPermission
    @AccessWithApiKey
    @Operation(summary = "Returns project by id")
    fun get(@PathVariable("projectId") projectId: Long): ProjectModel {
        return projectService.getView(projectId)?.let {
            projectModelAssembler.toModel(it)
        } ?: throw NotFoundException()
    }

    @GetMapping("/{projectId}/users")
    @Operation(summary = "Returns project all users, who have permission to access project")
    @AccessWithProjectPermission(Permission.ProjectPermissionType.MANAGE)
    fun getAllUsers(@PathVariable("projectId") projectId: Long,
                    pageable: Pageable,
                    @RequestParam("search", required = false) search: String?
    ): PagedModel<UserAccountInProjectModel> {
        return userAccountService.getAllInProject(projectId, pageable, search).let { users ->
            userArrayResourcesAssembler.toModel(users, userAccountInProjectModelAssembler)
        }
    }

    @PutMapping("/{projectId}/users/{userId}/set-permissions/{permissionType}")
    @AccessWithProjectPermission(Permission.ProjectPermissionType.MANAGE)
    @Operation(summary = "Sets user's direct permission")
    fun setUsersPermissions(
            @PathVariable("projectId") projectId: Long,
            @PathVariable("userId") userId: Long,
            @PathVariable("permissionType") permissionType: Permission.ProjectPermissionType,
    ) {
        if (userId == authenticationFacade.userAccount.id) {
            throw BadRequestException(Message.CANNOT_SET_YOUR_OWN_PERMISSIONS)
        }
        permissionService.setUserDirectPermission(projectId, userId, permissionType)
    }

    @PutMapping("/{projectId}/users/{userId}/revoke-access")
    @AccessWithProjectPermission(Permission.ProjectPermissionType.MANAGE)
    @Operation(summary = "Revokes user's access")
    fun revokePermission(
            @PathVariable("projectId") projectId: Long,
            @PathVariable("userId") userId: Long
    ) {
        if (userId == authenticationFacade.userAccount.id) {
            throw BadRequestException(Message.CAN_NOT_REVOKE_OWN_PERMISSIONS)
        }
        permissionService.revoke(projectId, userId)
    }

    @PostMapping(value = [""])
    @Operation(summary = "Creates project with specified languages")
    fun createProject(@RequestBody @Valid dto: CreateProjectDTO): ProjectModel {
        val userAccount = authenticationFacade.userAccount
        if (!this.tolgeeProperties.authentication.userCanCreateProjects
                && userAccount.role != UserAccount.Role.ADMIN) {
            throw PermissionException()
        }
        val project = projectService.createProject(dto)
        return projectModelAssembler.toModel(projectService.getView(project.id)!!)
    }

    @Operation(summary = "Modifies project")
    @PutMapping(value = ["/{projectId}"])
    @AccessWithProjectPermission(Permission.ProjectPermissionType.MANAGE)
    fun editProject(@RequestBody @Valid dto: EditProjectDTO): ProjectModel {
        val project = projectService.editProject(projectHolder.project.id, dto)
        return projectModelAssembler.toModel(projectService.getView(project.id)!!)
    }

    @DeleteMapping(value = ["/{projectId}"])
    @Operation(summary = "Deletes project by id")
    fun deleteProject(@PathVariable projectId: Long?) {
        securityService.checkProjectPermission(projectId!!, Permission.ProjectPermissionType.MANAGE)
        projectService.deleteProject(projectId)
    }

    @PutMapping("/{projectId}/invite")
    @Operation(summary = "Generates user invitation link for project")
    @AccessWithProjectPermission(Permission.ProjectPermissionType.MANAGE)
    fun inviteUser(@RequestBody @Valid invitation: ProjectInviteUserDto): String {
        val project = projectService.get(projectHolder.project.id).orElseThrow { NotFoundException() }!!
        return invitationService.create(project, invitation.type!!)
    }
}
