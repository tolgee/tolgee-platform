@file:Suppress("SpringElInspection")

package io.tolgee.service.security

import io.tolgee.constants.ComputedPermissionOrigin
import io.tolgee.constants.Message
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.project.LanguagePermissions
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.translationAgency.TranslationAgency
import io.tolgee.repository.PermissionRepository
import io.tolgee.service.CachedPermissionService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PermissionService(
  private val permissionRepository: PermissionRepository,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  @Lazy
  private val languageService: LanguageService,
  @Lazy
  private val userPreferencesService: UserPreferencesService,
  @Lazy
  private val applicationContext: ApplicationContext,
  private val entityManager: EntityManager,
) {
  @set:Autowired
  @set:Lazy
  lateinit var organizationService: OrganizationService

  @set:Autowired
  @set:Lazy
  lateinit var cachedPermissionService: CachedPermissionService

  @set:Lazy
  @set:Autowired
  lateinit var projectService: ProjectService

  @Transactional
  fun findPermissionNonCached(
    projectId: Long? = null,
    userId: Long? = null,
    organizationId: Long? = null,
  ): Permission? {
    return permissionRepository.findOneByProjectIdAndUserIdAndOrganizationId(
      projectId = projectId,
      userId = userId,
      organizationId = organizationId,
    )
  }

  fun getAllOfProject(project: Project?): Set<Permission> {
    return permissionRepository.getAllByProjectAndUserNotNull(project)
  }

  fun findById(id: Long): Permission? {
    return cachedPermissionService.find(id)
  }

  fun getProjectPermissionScopesNoApiKey(
    projectId: Long,
    userAccount: UserAccount,
  ) = getProjectPermissionScopesNoApiKey(projectId, userAccount.id)

  fun getProjectPermissionScopesNoApiKey(
    projectId: Long,
    userAccountId: Long,
  ): Array<Scope>? {
    return getProjectPermissionData(projectId, userAccountId).computedPermissions.expandedScopes
  }

  fun getProjectPermissionData(
    project: ProjectDto,
    userAccountId: Long,
  ): ProjectPermissionData {
    val projectPermission = find(projectId = project.id, userId = userAccountId)

    val organizationRole =
      project.organizationOwnerId
        .let { organizationRoleService.findType(userAccountId, it) }

    val organizationBasePermission =
      find(organizationId = project.organizationOwnerId)
        ?: throw IllegalStateException("Organization has no base permission")

    val computed =
      computeProjectPermission(
        organizationRole = organizationRole,
        organizationBasePermission = organizationBasePermission,
        directPermission = projectPermission,
        userAccountService.findDto(userAccountId)?.role ?: throw IllegalStateException("User not found"),
      )

    return ProjectPermissionData(
      organizationRole = organizationRole,
      organizationBasePermissions = organizationBasePermission,
      computedPermissions = computed,
      directPermissions = projectPermission,
      suggestionsMode = project.suggestionsMode,
    )
  }

  fun getUserProjectPermission(
    projectId: Long,
    userId: Long,
  ): PermissionDto? {
    return find(projectId, userId)
  }

  fun getPermittedTranslateLanguagesForUserIds(
    userIds: List<Long>,
    projectId: Long,
  ): Map<Long, List<Long>> {
    val data = permissionRepository.getUserPermittedLanguageIds(userIds, projectId)
    val result = mutableMapOf<Long, MutableList<Long>>()
    data.forEach {
      val languageIds =
        result.computeIfAbsent(it[0]) {
          mutableListOf()
        }
      languageIds.add(it[1])
    }
    return result
  }

  fun getPermittedTranslateLanguagesForProjectIds(
    projectIds: List<Long>,
    userId: Long,
  ): Map<Long, List<Long>> {
    val data = permissionRepository.getProjectPermittedLanguageIds(projectIds, userId)
    val result = mutableMapOf<Long, MutableList<Long>>()
    data.forEach {
      val languageIds =
        result.computeIfAbsent(it[0]) {
          mutableListOf()
        }
      languageIds.add(it[1])
    }
    return result
  }

  fun getProjectPermissionData(
    projectId: Long,
    userAccountId: Long,
  ): ProjectPermissionData {
    val project = projectService.findDto(projectId) ?: throw NotFoundException()
    return getProjectPermissionData(project, userAccountId)
  }

  fun create(permission: Permission): Permission {
    return cachedPermissionService.create(permission)
  }

  fun delete(permission: Permission) {
    cachedPermissionService.delete(permission)
    permission.user?.let {
      userPreferencesService.refreshPreferredOrganization(it.id)
    }
  }

  fun delete(permissionId: Long) {
    val permission = get(permissionId)
    delete(permission)
  }

  fun get(permissionId: Long): Permission {
    return this.cachedPermissionService.find(permissionId) ?: throw NotFoundException()
  }

  /**
   * Deletes all permissions in project
   * No need to evict cache, since this is only used when project is deleted
   */
  fun deleteAllByProject(projectId: Long) {
    val permissions = permissionRepository.getByProjectWithFetchedLanguages(projectId)
    permissionRepository.deleteAll(permissions)
  }

  @Transactional
  fun grantFullAccessToProject(
    userAccount: UserAccount,
    project: Project,
  ) {
    val permission =
      Permission(
        type = ProjectPermissionType.MANAGE,
        project = project,
        user = userAccount,
      )
    create(permission)
  }

  fun computeProjectPermission(
    organizationRole: OrganizationRoleType?,
    organizationBasePermission: IPermission,
    directPermission: IPermission?,
    userRole: UserAccount.Role? = null,
  ): ComputedPermissionDto {
    val computed =
      when {
        organizationRole == OrganizationRoleType.OWNER -> ComputedPermissionDto.ORGANIZATION_OWNER
        directPermission != null -> ComputedPermissionDto(directPermission, ComputedPermissionOrigin.DIRECT)
        organizationRole == OrganizationRoleType.MEMBER || organizationRole == OrganizationRoleType.MAINTAINER ->
          ComputedPermissionDto(
            organizationBasePermission,
            ComputedPermissionOrigin.ORGANIZATION_BASE,
          )

        else -> ComputedPermissionDto.NONE
      }

    return computed.getAdminOrSupporterPermissions(userRole)
  }

  fun createForInvitation(
    invitation: Invitation,
    params: CreateProjectInvitationParams,
  ): Permission {
    val type = params.type ?: throw IllegalStateException("Permission type cannot be null")

    validateLanguagePermissions(params.languagePermissions, type)

    val permission =
      Permission(
        invitation = invitation,
        project = params.project,
        type = type,
        agency =
          params.agencyId?.let {
            entityManager.getReference(TranslationAgency::class.java, it)
          },
      )

    setPermissionLanguages(permission, params.languagePermissions, params.project.id)

    return this.save(permission)
  }

  @Transactional
  fun find(
    projectId: Long? = null,
    userId: Long? = null,
    organizationId: Long? = null,
  ): PermissionDto? {
    return cachedPermissionService.find(projectId = projectId, userId = userId, organizationId = organizationId)
  }

  fun acceptInvitation(
    permission: Permission,
    userAccount: UserAccount,
  ): Permission {
    // switch user to the organization when accepted invitation
    userPreferencesService.setPreferredOrganization(permission.project!!.organizationOwner, userAccount)
    return cachedPermissionService.acceptInvitation(permission, userAccount)
  }

  fun setUserDirectPermission(
    projectId: Long,
    userId: Long,
    newPermissionType: ProjectPermissionType,
    languages: LanguagePermissions,
  ): Permission? {
    validateLanguagePermissions(
      languagePermissions = languages,
      newPermissionType = newPermissionType,
    )

    val permission = getOrCreateDirectPermission(projectId, userId)

    permission.scopes = emptyArray()
    permission.type = newPermissionType

    setPermissionLanguages(permission, languages, projectId)

    return this.save(permission)
  }

  fun getOrCreateDirectPermission(
    projectId: Long,
    userId: Long,
  ): Permission {
    val data = this.getProjectPermissionData(projectId, userId)

    checkUserIsInProject(data)

    data.organizationRole?.let {
      if (data.organizationRole == OrganizationRoleType.OWNER) {
        throw BadRequestException(Message.USER_IS_ORGANIZATION_OWNER)
      }
    }

    val permission =
      data.directPermissions?.let { findById(it.id) } ?: let {
        val userAccount = userAccountService.get(userId)
        val project = projectService.get(projectId)
        Permission(user = userAccount, project = project)
      }
    return permission
  }

  private fun checkUserIsInProject(data: ProjectPermissionData) {
    val hasOrganizationRole = data.organizationRole != null
    val hasDirectPermissions = data.directPermissions != null

    if (!hasDirectPermissions && !hasOrganizationRole) {
      throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)
    }
  }

  private fun Set<Language>?.standardize(): MutableSet<Language> {
    if (this === null) {
      return mutableSetOf()
    }
    return toMutableSet()
  }

  @Transactional
  fun setPermissionLanguages(
    permission: Permission,
    languagePermissions: LanguagePermissions,
    projectId: Long,
  ) {
    permission.translateLanguages = languagePermissions.translate.standardize()
    permission.stateChangeLanguages = languagePermissions.stateChange.standardize()
    permission.viewLanguages = languagePermissions.view.standardize()
    permission.suggestLanguages = languagePermissions.suggest.standardize()

    if (permission.viewLanguages.isNotEmpty()) {
      permission.viewLanguages.addAll(permission.translateLanguages)
      permission.viewLanguages.addAll(permission.stateChangeLanguages)
      permission.viewLanguages.addAll(permission.suggestLanguages)
    }
  }

  private fun validateLanguagePermissions(
    languagePermissions: LanguagePermissions,
    newPermissionType: ProjectPermissionType,
  ) {
    val isTranslate = newPermissionType == ProjectPermissionType.TRANSLATE
    val isReview = newPermissionType == ProjectPermissionType.REVIEW

    val hasTranslateLanguages = !languagePermissions.translate.isNullOrEmpty()
    val hasViewLanguages = !languagePermissions.view.isNullOrEmpty()
    val hasStateChangeLanguages = !languagePermissions.stateChange.isNullOrEmpty()

    if (hasViewLanguages) {
      throw BadRequestException(Message.CANNOT_SET_VIEW_LANGUAGES_WITHOUT_FOR_LEVEL_BASED_PERMISSIONS)
    }

    if (hasStateChangeLanguages && (!isReview)) {
      throw BadRequestException(Message.ONLY_REVIEW_PERMISSION_ACCEPTS_STATE_CHANGE_LANGUAGES)
    }

    if (hasTranslateLanguages && (!isTranslate && !isReview)) {
      throw BadRequestException(Message.ONLY_TRANSLATE_OR_REVIEW_PERMISSION_ACCEPTS_TRANSLATE_LANGUAGES)
    }

    if (isReview && (hasTranslateLanguages || hasStateChangeLanguages)) {
      val equal =
        languagePermissions.stateChange?.size == languagePermissions.translate?.size &&
          languagePermissions.stateChange?.containsAll(languagePermissions.translate ?: emptyList()) ?: false
      if (!equal) {
        throw BadRequestException(
          Message.CANNOT_SET_DIFFERENT_TRANSLATE_AND_STATE_CHANGE_LANGUAGES_FOR_LEVEL_BASED_PERMISSIONS,
        )
      }
    }
  }

  fun saveAll(permissions: Iterable<Permission>) {
    permissions.forEach { this.save(it) }
  }

  fun save(permission: Permission): Permission {
    return cachedPermissionService.save(permission)
  }

  fun revoke(
    userId: Long,
    projectId: Long,
  ) {
    val data = this.getProjectPermissionData(projectId, userId)
    if (data.organizationRole != null) {
      throw BadRequestException(Message.USER_IS_ORGANIZATION_MEMBER)
    }

    data.directPermissions?.let {
      findById(it.id)?.let { found ->
        cachedPermissionService.delete(found)
      }
    } ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)

    userPreferencesService.refreshPreferredOrganization(userId)
  }

  fun removeLanguageFromPermissions(language: Language) {
    val permissions = permissionRepository.findAllByPermittedLanguage(language)
    permissions.forEach { permission ->
      LanguageDeletedPermissionUpdater(applicationContext = applicationContext, permission, language).invoke()
    }
  }

  @Transactional
  fun leave(
    project: Project,
    userId: Long,
  ) {
    val permissionData = this.getProjectPermissionData(project.id, userId)
    if (permissionData.organizationRole != null) {
      throw BadRequestException(Message.CANNOT_LEAVE_PROJECT_WITH_ORGANIZATION_ROLE)
    }

    val directPermissions =
      permissionData.directPermissions
        ?: throw BadRequestException(Message.DONT_HAVE_DIRECT_PERMISSIONS)

    val permissionEntity =
      this.findById(directPermissions.id)
        ?: throw NotFoundException()

    this.delete(permissionEntity)
  }

  fun getPermittedViewLanguages(
    projectId: Long,
    userId: Long,
  ): Collection<LanguageDto> {
    val permissionData = this.getProjectPermissionData(projectId, userId)

    val allLanguages = languageService.findAll(projectId)
    val viewLanguageIds = permissionData.computedPermissions.viewLanguageIds

    val permittedLanguages =
      if (viewLanguageIds.isNullOrEmpty()) {
        allLanguages
      } else {
        allLanguages.filter {
          viewLanguageIds.contains(
            it.id,
          )
        }
      }

    return permittedLanguages
  }

  @Transactional
  fun removeDirectProjectPermissions(
    projectId: Long,
    userId: Long,
  ) {
    val permission = getProjectPermissionData(projectId, userId).directPermissions ?: return
    delete(permission.id)
  }

  fun removeAllProjectInOrganization(
    organizationId: Long,
    userId: Long,
  ): List<Permission> {
    val permissions = permissionRepository.findAllByOrganizationAndUserId(organizationId, userId)
    permissions.forEach { delete(it) }
    return permissions
  }

  fun deleteAll(permissions: List<Permission>) {
    permissionRepository.deleteAll(permissions)
  }

  fun getAgencyPermissions(agencyId: Long): List<Permission> {
    return permissionRepository.findAllByAgencyId(agencyId)
  }
}
