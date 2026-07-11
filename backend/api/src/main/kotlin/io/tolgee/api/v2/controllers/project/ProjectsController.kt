/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Message
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.dtos.request.project.ProjectFilters
import io.tolgee.dtos.request.project.SetPermissionLanguageParams
import io.tolgee.dtos.request.task.UserAccountFilters
import io.tolgee.exceptions.BadRequestException
import io.tolgee.facade.ProjectPermissionFacade
import io.tolgee.facade.ProjectWithStatsFacade
import io.tolgee.hateoas.project.ProjectModel
import io.tolgee.hateoas.project.ProjectModelAssembler
import io.tolgee.hateoas.project.ProjectWithStatsModel
import io.tolgee.hateoas.userAccount.UserAccountInProjectModel
import io.tolgee.hateoas.userAccount.UserAccountInProjectModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.ExtendedUserAccountInProject
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.IsGlobalRoute
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.ImageUploadService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.project.ProjectCreationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Suppress("MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Projects")
@OpenApiOrderExtension(1)
class ProjectsController(
  private val projectService: ProjectService,
  private val projectHolder: ProjectHolder,
  private val arrayResourcesAssembler: PagedResourcesAssembler<ProjectWithLanguagesView>,
  private val userArrayResourcesAssembler: PagedResourcesAssembler<ExtendedUserAccountInProject>,
  private val userAccountInProjectModelAssembler: UserAccountInProjectModelAssembler,
  private val projectModelAssembler: ProjectModelAssembler,
  private val userAccountService: UserAccountService,
  private val permissionService: PermissionService,
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val imageUploadService: ImageUploadService,
  private val projectPermissionFacade: ProjectPermissionFacade,
  private val projectWithStatsFacade: ProjectWithStatsFacade,
  private val projectCreationService: ProjectCreationService,
) {
  @PostMapping(value = [""])
  @Operation(summary = "Create project", description = "Creates a new project with languages and initial settings.")
  @RequestActivity(ActivityType.CREATE_PROJECT)
  @IsGlobalRoute
  @AllowApiAccess(tokenType = AuthTokenType.ONLY_PAT)
  @OpenApiOrderExtension(1)
  fun createProject(
    @RequestBody @Valid
    dto: CreateProjectRequest,
  ): ProjectModel {
    organizationRoleService.checkUserCanCreateProject(dto.organizationId)
    val project = projectCreationService.createProject(dto)
    if (organizationRoleService.getType(dto.organizationId) == OrganizationRoleType.MAINTAINER) {
      // Maintainers get full access to projects they create
      permissionService.grantFullAccessToProject(authenticationFacade.authenticatedUserEntity, project)
    }
    return projectModelAssembler.toModel(projectService.getView(project.id))
  }

  @GetMapping("/{projectId:[0-9]+}")
  @Operation(summary = "Get one project")
  @UseDefaultPermissions
  @AllowApiAccess
  @OpenApiOrderExtension(2)
  fun get(): ProjectModel {
    return projectService.getView(projectHolder.project.id).let {
      projectModelAssembler.toModel(it)
    }
  }

  @Operation(summary = "Get all permitted", description = "Returns all projects where current user has any permission")
  @GetMapping("")
  @IsGlobalRoute
  @AllowApiAccess(tokenType = AuthTokenType.ONLY_PAT)
  @OpenApiOrderExtension(3)
  fun getAll(
    @ParameterObject
    filters: ProjectFilters,
    @ParameterObject
    @SortDefault("name")
    pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<ProjectModel> {
    val projects = projectService.findPermittedInOrganizationPaged(pageable, search, filters = filters)
    return arrayResourcesAssembler.toModel(projects, projectModelAssembler)
  }

  @Operation(summary = "Update project settings")
  @PutMapping(value = ["/{projectId:[0-9]+}"])
  @RequestActivity(ActivityType.EDIT_PROJECT)
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequiresSuperAuthentication
  @AllowApiAccess
  @OpenApiOrderExtension(4)
  fun editProject(
    @RequestBody @Valid
    dto: EditProjectRequest,
  ): ProjectModel {
    val project = projectService.editProject(projectHolder.project.id, dto)
    return projectModelAssembler.toModel(projectService.getView(project.id))
  }

  @DeleteMapping(value = ["/{projectId:[0-9]+}"])
  @Operation(summary = "Delete project")
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @RequiresSuperAuthentication
  @AllowApiAccess
  @OpenApiOrderExtension(5)
  fun deleteProject(
    @PathVariable projectId: Long,
  ) {
    projectService.deleteProject(projectId)
  }

  @Operation(
    summary = "Get all with stats",
    description = "Returns all projects (including statistics) where current user has any permission",
  )
  @GetMapping("/with-stats")
  @IsGlobalRoute
  fun getAllWithStatistics(
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<ProjectWithStatsModel> {
    val projects = projectService.findPermittedInOrganizationPaged(pageable, search)
    return projectWithStatsFacade.getPagedModelWithStats(projects)
  }

  @GetMapping("/{projectId:[0-9]+}/users")
  @Operation(
    summary = "Get users with project access",
    description = "Returns all project users, who have permission to access project",
  )
  @RequiresProjectPermissions([Scope.MEMBERS_VIEW])
  @RequiresSuperAuthentication
  @AllowApiAccess
  fun getAllUsers(
    @PathVariable("projectId") projectId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
    @ParameterObject filters: UserAccountFilters = UserAccountFilters(),
  ): PagedModel<UserAccountInProjectModel> {
    return userAccountService
      .getAllInProjectWithPermittedLanguages(
        projectId,
        pageable,
        search,
        filters = filters,
      ).let { users ->
        userArrayResourcesAssembler.toModel(users, userAccountInProjectModelAssembler)
      }
  }

  @PutMapping("/{projectId:[0-9]+}/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Upload project avatar")
  @ResponseStatus(HttpStatus.OK)
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
    @PathVariable projectId: Long,
  ): ProjectModel {
    imageUploadService.validateIsImage(avatar)
    projectService.setAvatar(projectHolder.projectEntity, avatar.inputStream)
    return projectModelAssembler.toModel(projectService.getView(projectId))
  }

  @DeleteMapping("/{projectId:[0-9]+}/avatar")
  @Operation(summary = "Delete project avatar")
  @ResponseStatus(HttpStatus.OK)
  @RequiresProjectPermissions([Scope.PROJECT_EDIT])
  @AllowApiAccess
  fun removeAvatar(
    @PathVariable projectId: Long,
  ): ProjectModel {
    projectService.removeAvatar(projectHolder.projectEntity)
    return projectModelAssembler.toModel(projectService.getView(projectId))
  }

  @PutMapping("/{projectId:[0-9]+}/users/{userId}/set-permissions/{permissionType}")
  @Operation(summary = "Set direct permission to user")
  @RequiresProjectPermissions([Scope.MEMBERS_EDIT])
  @RequiresSuperAuthentication
  fun setUsersPermissions(
    @PathVariable("userId") userId: Long,
    @PathVariable("permissionType") permissionType: ProjectPermissionType,
    @ParameterObject params: SetPermissionLanguageParams,
  ) {
    projectPermissionFacade.checkNotCurrentUser(userId)
    permissionService.setUserDirectPermission(
      projectId = projectHolder.project.id,
      userId = userId,
      newPermissionType = permissionType,
      languages = projectPermissionFacade.getLanguages(params, projectHolder.project.id),
    )
  }

  @PutMapping("/{projectId:[0-9]+}/users/{userId}/set-by-organization")
  @Operation(
    summary = "Remove direct project permission",
    description =
      "Removes user's direct project permission, explicitly set for the project. " +
        "User will have now base permissions from organization or no permission if they're not organization member.",
  )
  @RequiresProjectPermissions([Scope.MEMBERS_EDIT])
  @RequiresSuperAuthentication
  fun removeDirectProjectPermissions(
    @PathVariable("userId") userId: Long,
  ) {
    projectPermissionFacade.checkNotCurrentUser(userId)
    permissionService.removeDirectProjectPermissions(
      projectId = projectHolder.project.id,
      userId = userId,
    )
  }

  @PutMapping("/{projectId:[0-9]+}/users/{userId}/revoke-access")
  @Operation(summary = "Revoke project access")
  @RequiresProjectPermissions([Scope.MEMBERS_EDIT])
  @RequiresSuperAuthentication
  fun revokePermission(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("userId") userId: Long,
  ) {
    if (userId == authenticationFacade.authenticatedUser.id) {
      throw BadRequestException(Message.CAN_NOT_REVOKE_OWN_PERMISSIONS)
    }
    permissionService.revoke(userId, projectId)
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/leave"])
  @Operation(summary = "Leave project")
  @UseDefaultPermissions
  @RequiresSuperAuthentication
  fun leaveProject() {
    permissionService.leave(projectHolder.projectEntity, authenticationFacade.authenticatedUser.id)
  }
}
