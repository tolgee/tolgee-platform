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
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Permission
import io.tolgee.model.views.ProjectView
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.PermissionService
import io.tolgee.service.ProjectService
import io.tolgee.service.UserAccountService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Projects")
class V2ProjectsController(
        val projectService: ProjectService,
        val projectHolder: ProjectHolder,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        val arrayResourcesAssembler: PagedResourcesAssembler<ProjectView>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        val userArrayResourcesAssembler: PagedResourcesAssembler<UserAccountInProjectView>,
        val userAccountInProjectModelAssembler: UserAccountInProjectModelAssembler,
        val projectModelAssembler: ProjectModelAssembler,
        val userAccountService: UserAccountService,
        val permissionService: PermissionService,
        val authenticationFacade: AuthenticationFacade
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
        } ?: throw io.tolgee.exceptions.NotFoundException()
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

}
