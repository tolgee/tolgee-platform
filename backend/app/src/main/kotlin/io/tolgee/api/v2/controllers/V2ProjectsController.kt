/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.key.LanguageConfigItemModelAssembler
import io.tolgee.api.v2.hateoas.machineTranslation.LanguageConfigItemModel
import io.tolgee.api.v2.hateoas.project.ProjectModel
import io.tolgee.api.v2.hateoas.project.ProjectModelAssembler
import io.tolgee.api.v2.hateoas.project.ProjectTransferOptionModel
import io.tolgee.api.v2.hateoas.project.ProjectWithStatsModel
import io.tolgee.api.v2.hateoas.project.ProjectWithStatsModelAssembler
import io.tolgee.api.v2.hateoas.user_account.UserAccountInProjectModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountInProjectModelAssembler
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.AutoTranslationSettingsDto
import io.tolgee.dtos.request.SetMachineTranslationSettingsDto
import io.tolgee.dtos.request.project.CreateProjectDTO
import io.tolgee.dtos.request.project.EditProjectDTO
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Permission
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ProjectView
import io.tolgee.model.views.ProjectWithStatsView
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.api_key_auth.AccessWithApiKey
import io.tolgee.security.project_auth.AccessWithAnyProjectPermission
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.AutoTranslationService
import io.tolgee.service.ImageUploadService
import io.tolgee.service.InvitationService
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.OrganizationService
import io.tolgee.service.PermissionService
import io.tolgee.service.ProjectService
import io.tolgee.service.SecurityService
import io.tolgee.service.UserAccountService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.data.domain.PageImpl
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
  private val projectWithStatsModelAssembler: ProjectWithStatsModelAssembler,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val arrayWithStatsResourcesAssembler: PagedResourcesAssembler<ProjectWithStatsView>,
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  private val languageConfigItemModelAssembler: LanguageConfigItemModelAssembler,
  private val userAccountService: UserAccountService,
  private val permissionService: PermissionService,
  private val authenticationFacade: AuthenticationFacade,
  private val tolgeeProperties: TolgeeProperties,
  private val securityService: SecurityService,
  private val invitationService: InvitationService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val projectMachineTranslationServiceConfigService: MtServiceConfigService,
  private val imageUploadService: ImageUploadService,
  private val mtServiceConfigService: MtServiceConfigService,
  private val autoTranslateService: AutoTranslationService
) {
  @Operation(summary = "Returns all projects where current user has any permission")
  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  fun getAll(
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?
  ): PagedModel<ProjectModel> {
    val projects = projectService.findPermittedPaged(pageable, search)
    return arrayResourcesAssembler.toModel(projects, projectModelAssembler)
  }

  @Operation(summary = "Returns all projects (includingStatistics) where current user has any permission")
  @GetMapping("/with-stats", produces = [MediaTypes.HAL_JSON_VALUE])
  fun getAllWithStatistics(
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?
  ): PagedModel<ProjectWithStatsModel> {
    val projects = projectService.findPermittedPaged(pageable, search)
    val projectIds = projects.content.map { it.id }
    val stats = projectService.getProjectsStatistics(projectIds).associateBy { it.projectId }
    val languages = projectService.getProjectsWithFetchedLanguages(projectIds)
      .associate { it.id to it.languages.toList() }
    val projectsWithStatsContent = projects.content.map { ProjectWithStatsView(it, stats[it.id]!!, languages[it.id]!!) }
    val page = PageImpl(projectsWithStatsContent, projects.pageable, projects.totalElements)
    return arrayWithStatsResourcesAssembler.toModel(page, projectWithStatsModelAssembler)
  }

  @GetMapping("/{projectId}")
  @AccessWithAnyProjectPermission
  @AccessWithApiKey
  @Operation(summary = "Returns project by id")
  fun get(@PathVariable("projectId") projectId: Long): ProjectModel {
    return projectService.findView(projectId)?.let {
      projectModelAssembler.toModel(it)
    } ?: throw NotFoundException()
  }

  @GetMapping("/{projectId}/users")
  @Operation(summary = "Returns project all users, who have permission to access project")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  fun getAllUsers(
    @PathVariable("projectId") projectId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?
  ): PagedModel<UserAccountInProjectModel> {
    return userAccountService.getAllInProject(projectId, pageable, search).let { users ->
      userArrayResourcesAssembler.toModel(users, userAccountInProjectModelAssembler)
    }
  }

  @PutMapping("/{id:[0-9]+}/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Uploads organizations avatar")
  @ResponseStatus(HttpStatus.OK)
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
    @PathVariable id: Long
  ): ProjectModel {
    imageUploadService.validateIsImage(avatar)
    projectService.setAvatar(projectHolder.projectEntity, avatar.inputStream)
    return projectModelAssembler.toModel(projectService.getView(id))
  }

  @DeleteMapping("/{id:[0-9]+}/avatar")
  @Operation(summary = "Deletes organization avatar")
  @ResponseStatus(HttpStatus.OK)
  fun removeAvatar(
    @PathVariable id: Long
  ): ProjectModel {
    projectService.removeAvatar(projectHolder.projectEntity)
    return projectModelAssembler.toModel(projectService.getView(id))
  }

  @PutMapping("/{projectId}/users/{userId}/set-permissions/{permissionType}")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  @Operation(summary = "Sets user's direct permission")
  fun setUsersPermissions(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("userId") userId: Long,
    @PathVariable("permissionType") permissionType: ProjectPermissionType,
  ) {
    if (userId == authenticationFacade.userAccount.id) {
      throw BadRequestException(io.tolgee.constants.Message.CANNOT_SET_YOUR_OWN_PERMISSIONS)
    }
    permissionService.setUserDirectPermission(projectId, userId, permissionType)
  }

  @PutMapping("/{projectId}/users/{userId}/revoke-access")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  @Operation(summary = "Revokes user's access")
  fun revokePermission(
    @PathVariable("projectId") projectId: Long,
    @PathVariable("userId") userId: Long
  ) {
    if (userId == authenticationFacade.userAccount.id) {
      throw BadRequestException(io.tolgee.constants.Message.CAN_NOT_REVOKE_OWN_PERMISSIONS)
    }
    permissionService.revoke(projectId, userId)
  }

  @PostMapping(value = [""])
  @Operation(summary = "Creates project with specified languages")
  fun createProject(@RequestBody @Valid dto: CreateProjectDTO): ProjectModel {
    val userAccount = authenticationFacade.userAccount
    if (!this.tolgeeProperties.authentication.userCanCreateProjects &&
      userAccount.role != UserAccount.Role.ADMIN
    ) {
      throw PermissionException()
    }
    val project = projectService.createProject(dto)
    return projectModelAssembler.toModel(projectService.findView(project.id)!!)
  }

  @Operation(summary = "Modifies project")
  @PutMapping(value = ["/{projectId}"])
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  fun editProject(@RequestBody @Valid dto: EditProjectDTO): ProjectModel {
    val project = projectService.editProject(projectHolder.project.id, dto)
    return projectModelAssembler.toModel(projectService.findView(project.id)!!)
  }

  @DeleteMapping(value = ["/{projectId}"])
  @Operation(summary = "Deletes project by id")
  fun deleteProject(@PathVariable projectId: Long) {
    securityService.checkProjectPermission(projectId, ProjectPermissionType.MANAGE)
    projectService.deleteProject(projectId)
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/transfer-to-organization/{organizationId:[0-9]+}"])
  @Operation(summary = "Transfers project's ownership to organization")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  fun transferProjectToOrganization(@PathVariable projectId: Long, @PathVariable organizationId: Long) {
    organizationRoleService.checkUserIsOwner(organizationId)
    projectService.transferToOrganization(projectId, organizationId)
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/transfer-to-user/{userId:[0-9]+}"])
  @Operation(summary = "Transfers project's ownership to user")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  fun transferProjectToUser(@PathVariable projectId: Long, @PathVariable userId: Long) {
    securityService.checkAnyProjectPermission(projectId, userId)
    projectService.transferToUser(projectId, userId)
  }

  @PutMapping(value = ["/{projectId:[0-9]+}/leave"])
  @Operation(summary = "Leave project")
  @AccessWithProjectPermission(ProjectPermissionType.VIEW)
  fun leaveProject(@PathVariable projectId: Long) {
    val project = projectHolder.projectEntity
    if (project.userOwner?.id == authenticationFacade.userAccount.id) {
      throw BadRequestException(io.tolgee.constants.Message.CANNOT_LEAVE_OWNING_PROJECT)
    }
    val permissionData = permissionService.getProjectPermissionData(project.id, authenticationFacade.userAccount.id)
    if (permissionData.organizationRole != null) {
      throw BadRequestException(io.tolgee.constants.Message.CANNOT_LEAVE_PROJECT_WITH_ORGANIZATION_ROLE)
    }

    val directPermissions = permissionData.directPermissions
      ?: throw BadRequestException(io.tolgee.constants.Message.DONT_HAVE_DIRECT_PERMISSIONS)

    val permissionEntity = permissionService.findById(directPermissions.id)
      ?: throw NotFoundException()

    permissionService.delete(permissionEntity)
  }

  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  @Operation(summary = "Returns transfer option")
  @GetMapping(value = ["/{projectId:[0-9]+}/transfer-options"])
  fun getTransferOptions(@RequestParam search: String? = ""): CollectionModel<ProjectTransferOptionModel> {
    val project = projectHolder.project
    val organizations = organizationService.findPermittedPaged(
      PageRequest.of(0, 10),
      true,
      search,
      project.organizationOwnerId
    )
    val options = organizations.content.map {
      ProjectTransferOptionModel(
        name = it.name,
        id = it.id,
        type = ProjectTransferOptionModel.TransferOptionType.ORGANIZATION
      )
    }.toMutableList()
    val users = userAccountService.getAllInProject(
      projectId = project.id,
      PageRequest.of(0, 10),
      search,
      project.userOwnerId
    )
    options.addAll(
      users.content.map {
        ProjectTransferOptionModel(
          name = it.name,
          username = it.username,
          id = it.id,
          type = ProjectTransferOptionModel.TransferOptionType.USER
        )
      }
    )
    options.sortBy { it.name }
    return CollectionModel.of(options)
  }

  @PutMapping("/{projectId}/invite")
  @Operation(summary = "Generates user invitation link for project")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  fun inviteUser(@RequestBody @Valid invitation: ProjectInviteUserDto): String {
    val project = projectService.get(projectHolder.project.id)
    return invitationService.create(project, invitation.type!!)
  }

  @GetMapping("/{projectId}/machine-translation-service-settings")
  @Operation(summary = "Returns machine translation settings for project")
  @AccessWithProjectPermission(ProjectPermissionType.VIEW)
  fun getMachineTranslationSettings(): CollectionModel<LanguageConfigItemModel> {
    val data = mtServiceConfigService.getProjectSettings(projectHolder.projectEntity)
    return languageConfigItemModelAssembler.toCollectionModel(data)
  }

  @PutMapping("/{projectId}/machine-translation-service-settings")
  @AccessWithProjectPermission(ProjectPermissionType.MANAGE)
  @Operation(summary = "Sets machine translation settings for project")
  fun setMachineTranslationSettings(
    @RequestBody dto: SetMachineTranslationSettingsDto
  ): CollectionModel<LanguageConfigItemModel> {
    mtServiceConfigService.setProjectSettings(projectHolder.projectEntity, dto)
    return getMachineTranslationSettings()
  }

  @PutMapping("/{projectId}/auto-translation-settings")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.MANAGE)
  @Operation(summary = "Sets auto translation settings for project")
  fun setAutoTranslationSettings(
    @RequestBody dto: AutoTranslationSettingsDto
  ): AutoTranslationSettingsDto {
    autoTranslateService.saveConfig(projectHolder.projectEntity, dto)
    return dto
  }

  @GetMapping("/{projectId}/auto-translation-settings")
  @AccessWithProjectPermission(Permission.ProjectPermissionType.MANAGE)
  @Operation(summary = "Returns auto translation settings for project")
  fun getAutoTranslationSettings(): AutoTranslationSettingsDto {
    val config = autoTranslateService.getConfig(projectHolder.projectEntity)
    return AutoTranslationSettingsDto(
      usingTranslationMemory = config.usingTm,
      usingMachineTranslation = config.usingPrimaryMtService
    )
  }
}
