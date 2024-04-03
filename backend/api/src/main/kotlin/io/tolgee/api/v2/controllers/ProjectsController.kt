/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Message
import io.tolgee.dtos.request.AutoTranslationSettingsDto
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.dtos.request.project.SetPermissionLanguageParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.facade.ProjectPermissionFacade
import io.tolgee.facade.ProjectWithStatsFacade
import io.tolgee.hateoas.autoTranslationConfig.AutoTranslationConfigModel
import io.tolgee.hateoas.autoTranslationConfig.AutoTranslationSettingsModelAssembler
import io.tolgee.hateoas.invitation.ProjectInvitationModel
import io.tolgee.hateoas.invitation.ProjectInvitationModelAssembler
import io.tolgee.hateoas.project.ProjectModel
import io.tolgee.hateoas.project.ProjectModelAssembler
import io.tolgee.hateoas.project.ProjectTransferOptionModel
import io.tolgee.hateoas.project.ProjectWithStatsModel
import io.tolgee.hateoas.userAccount.UserAccountInProjectModel
import io.tolgee.hateoas.userAccount.UserAccountInProjectModelAssembler
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
import io.tolgee.service.InvitationService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.translation.AutoTranslationService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.MediaTypes
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

@Suppress(names = ["MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection"])
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Projects")
@OpenApiOrderExtension(-100)
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
  private val invitationService: InvitationService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val imageUploadService: ImageUploadService,
  private val projectInvitationModelAssembler: ProjectInvitationModelAssembler,
  private val autoTranslateService: AutoTranslationService,
  private val projectPermissionFacade: ProjectPermissionFacade,
  private val projectWithStatsFacade: ProjectWithStatsFacade,
  private val autoTranslationSettingsModelAssembler: AutoTranslationSettingsModelAssembler,
) {
  @Operation(summary = "Get all permitted", description = "Returns all projects where current user has any permission")
  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  @IsGlobalRoute
  @AllowApiAccess(tokenType = AuthTokenType.ONLY_PAT)
  @OpenApiOrderExtension(-100)
  fun getAll(
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<ProjectModel> {
    val projects = projectService.findPermittedInOrganizationPaged(pageable, search)
    return arrayResourcesAssembler.toModel(projects, projectModelAssembler)
  }

  @Operation(
    summary = "Get all with stats",
    description = "Returns all projects (including statistics) where current user has any permission",
  )
  @GetMapping("/with-stats", produces = [MediaTypes.HAL_JSON_VALUE])
  @IsGlobalRoute
  fun getAllWithStatistics(
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<ProjectWithStatsModel> {
    val projects = projectService.findPermittedInOrganizationPaged(pageable, search)
    return projectWithStatsFacade.getPagedModelWithStats(projects)
  }

  @GetMapping("/{projectId}")
  @Operation(summary = "Get one project")
  @UseDefaultPermissions
  @AllowApiAccess
  fun get(
    @PathVariable("projectId") projectId: Long,
  ): ProjectModel {
    return projectService.getView(projectId).let {
      projectModelAssembler.toModel(it)
    }
  }

  @GetMapping("/{projectId}/users")
  @Operation(
    summary = "Users with project access",
    description = "Returns all project users, who have permission to access project",
  )
  @RequiresProjectPermissions([ Scope.MEMBERS_VIEW ])
  @RequiresSuperAuthentication
  @AllowApiAccess
  fun getAllUsers(
    @PathVariable("projectId") projectId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
  ): PagedModel<UserAccountInProjectModel> {
    return userAccountService.getAllInProjectWithPermittedLanguages(projectId, pageable, search).let { users ->
      userArrayResourcesAssembler.toModel(users, userAccountInProjectModelAssembler)
    }
  }

  @PutMapping("/{projectId:[0-9]+}/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Upload project avatar")
  @ResponseStatus(HttpStatus.OK)
  @RequiresProjectPermissions([ Scope.PROJECT_EDIT ])
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
  @RequiresProjectPermissions([ Scope.PROJECT_EDIT ])
  @AllowApiAccess
  fun removeAvatar(
    @PathVariable projectId: Long,
  ): ProjectModel {
    projectService.removeAvatar(projectHolder.projectEntity)
    return projectModelAssembler.toModel(projectService.getView(projectId))
  }

  @PutMapping("/{projectId}/users/{userId}/set-permissions/{permissionType}")
  @Operation(summary = "Set direct permission to user")
  @RequiresProjectPermissions([ Scope.MEMBERS_EDIT ])
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

  @PutMapping("/{projectId}/users/{userId}/set-by-organization")
  @Operation(
    summary = "Remove explicit project permission",
    description = "Removes user's explicit project permission. User will have base permissions from organization.",
  )
  @RequiresProjectPermissions([ Scope.MEMBERS_EDIT ])
  @RequiresSuperAuthentication
  fun setOrganizationBase(
    @PathVariable("userId") userId: Long,
  ) {
    projectPermissionFacade.checkNotCurrentUser(userId)
    permissionService.setOrganizationBasePermissions(
      projectId = projectHolder.project.id,
      userId = userId,
    )
  }

  @PutMapping("/{projectId}/users/{userId}/revoke-access")
  @Operation(summary = "Revoke project access")
  @RequiresProjectPermissions([ Scope.MEMBERS_EDIT ])
  @RequiresSuperAuthentication
  fun revokePermission(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("userId") userId: Long,
  ) {
    if (userId == authenticationFacade.authenticatedUser.id) {
      throw BadRequestException(Message.CAN_NOT_REVOKE_OWN_PERMISSIONS)
    }
    permissionService.revoke(projectId, userId)
  }

  @PostMapping(value = [""])
  @Operation(summary = "Create project", description = "Creates a new project with languages and initial settings.")
  @RequestActivity(ActivityType.CREATE_PROJECT)
  @IsGlobalRoute
  @AllowApiAccess(tokenType = AuthTokenType.ONLY_PAT)
  fun createProject(
    @RequestBody @Valid
    dto: CreateProjectRequest,
  ): ProjectModel {
    organizationRoleService.checkUserIsOwner(dto.organizationId)
    val project = projectService.createProject(dto)
    return projectModelAssembler.toModel(projectService.getView(project.id))
  }

  @Operation(summary = "Update project settings")
  @PutMapping(value = ["/{projectId}"])
  @RequestActivity(ActivityType.EDIT_PROJECT)
  @RequiresProjectPermissions([ Scope.PROJECT_EDIT ])
  @RequiresSuperAuthentication
  @AllowApiAccess
  fun editProject(
    @RequestBody @Valid
    dto: EditProjectRequest,
  ): ProjectModel {
    val project = projectService.editProject(projectHolder.project.id, dto)
    return projectModelAssembler.toModel(projectService.getView(project.id))
  }

  @DeleteMapping(value = ["/{projectId}"])
  @Operation(summary = "Delete project")
  @RequiresProjectPermissions([ Scope.PROJECT_EDIT ])
  @RequiresSuperAuthentication
  @AllowApiAccess
  fun deleteProject(
    @PathVariable projectId: Long,
  ) {
    projectService.deleteProject(projectId)
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/transfer-to-organization/{organizationId:[0-9]+}"])
  @Operation(summary = "Transfer project", description = "Transfers project's ownership to organization")
  @RequiresProjectPermissions([ Scope.PROJECT_EDIT ])
  @RequiresSuperAuthentication
  fun transferProjectToOrganization(
    @PathVariable projectId: Long,
    @PathVariable organizationId: Long,
  ) {
    organizationRoleService.checkUserIsOwner(organizationId)
    projectService.transferToOrganization(projectId, organizationId)
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/leave"])
  @Operation(summary = "Leave project")
  @UseDefaultPermissions
  @RequiresSuperAuthentication
  fun leaveProject() {
    permissionService.leave(projectHolder.projectEntity, authenticationFacade.authenticatedUser.id)
  }

  @GetMapping(value = ["/{projectId:[0-9]+}/transfer-options"])
  @Operation(summary = "Get transfer option")
  @RequiresProjectPermissions([ Scope.PROJECT_EDIT ])
  fun getTransferOptions(
    @RequestParam search: String? = "",
  ): CollectionModel<ProjectTransferOptionModel> {
    val project = projectHolder.project
    val organizations =
      organizationService.findPermittedPaged(
        PageRequest.of(0, 10),
        true,
        search,
        project.organizationOwnerId,
      )
    val options =
      organizations.content.map {
        ProjectTransferOptionModel(
          name = it.name,
          slug = it.slug,
          id = it.id,
        )
      }.toMutableList()
    options.sortBy { it.name }
    return CollectionModel.of(options)
  }

  @GetMapping("{projectId:[0-9]+}/invitations")
  @Operation(summary = "Get project invitations")
  @RequiresProjectPermissions([ Scope.MEMBERS_VIEW ])
  @RequiresSuperAuthentication
  @AllowApiAccess
  fun getProjectInvitations(
    @PathVariable("projectId") id: Long,
  ): CollectionModel<ProjectInvitationModel> {
    val project = projectService.get(id)
    val invitations = invitationService.getForProject(project)
    return projectInvitationModelAssembler.toCollectionModel(invitations)
  }

  @PutMapping("/{projectId}/per-language-auto-translation-settings")
  @Operation(
    summary = "Set per-language auto-translation settings",
  )
  @RequiresProjectPermissions([ Scope.LANGUAGES_EDIT ])
  @AllowApiAccess
  fun setPerLanguageAutoTranslationSettings(
    @RequestBody dto: List<AutoTranslationSettingsDto>,
  ): CollectionModel<AutoTranslationConfigModel> {
    val config = autoTranslateService.saveConfig(projectHolder.projectEntity, dto)
    return autoTranslationSettingsModelAssembler.toCollectionModel(config)
  }

  @GetMapping("/{projectId}/per-language-auto-translation-settings")
  @Operation(summary = "Get per-language auto-translation settings")
  @UseDefaultPermissions
  @AllowApiAccess
  fun getPerLanguageAutoTranslationSettings(): CollectionModel<AutoTranslationConfigModel> {
    val configs = autoTranslateService.getConfigs(projectHolder.projectEntity)
    return autoTranslationSettingsModelAssembler.toCollectionModel(configs)
  }

  @PutMapping("/{projectId}/auto-translation-settings")
  @Operation(
    summary = "Set default auto translation settings for project",
    description =
      "Sets default auto-translation settings for project " +
        "(deprecated: use per language config with null language id)",
    deprecated = true,
  )
  @RequiresProjectPermissions([ Scope.LANGUAGES_EDIT ])
  @AllowApiAccess
  fun setAutoTranslationSettings(
    @RequestBody dto: AutoTranslationSettingsDto,
  ): AutoTranslationConfigModel {
    val config = autoTranslateService.saveDefaultConfig(projectHolder.projectEntity, dto)
    return autoTranslationSettingsModelAssembler.toModel(config)
  }

  @GetMapping("/{projectId}/auto-translation-settings")
  @Operation(
    summary = "Get default auto-translation settings for project",
    description =
      "Returns default auto translation settings for project " +
        "(deprecated: use per language config with null language id)",
    deprecated = true,
  )
  @UseDefaultPermissions
  @AllowApiAccess
  fun getAutoTranslationSettings(): AutoTranslationConfigModel {
    val config = autoTranslateService.getDefaultConfig(projectHolder.projectEntity)
    return autoTranslationSettingsModelAssembler.toModel(config)
  }
}
