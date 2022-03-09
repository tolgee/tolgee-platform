@file:Suppress("SpringElInspection")

package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.repository.PermissionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PermissionService(
  private val permissionRepository: PermissionRepository,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
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

  fun getAllOfProject(project: Project?): Set<Permission> {
    return permissionRepository.getAllByProjectAndUserNotNull(project)
  }

  fun findById(id: Long): Permission? {
    return cachedPermissionService.findById(id)
  }

  fun getProjectPermissionType(projectId: Long, userAccount: UserAccount) =
    getProjectPermissionType(projectId, userAccount.id)

  fun getProjectPermissionType(projectId: Long, userAccountId: Long): ProjectPermissionType? {
    return getProjectPermissionData(projectId, userAccountId).computedPermissions.type
  }

  fun getProjectPermissionData(project: ProjectDto, userAccountId: Long): ProjectPermissionData {
    val projectPermission = findOneDtoByProjectIdAndUserId(project.id, userAccountId)

    val organizationRole = project.organizationOwnerId
      ?.let { organizationRoleService.findType(userAccountId, it) }

    val organizationBasePermissionType = project.organizationOwnerId?.let {
      organizationService.find(it)?.basePermissions ?: throw NotFoundException()
    }

    val computed = computeProjectPermissionType(
      organizationRole = organizationRole,
      organizationBasePermissionType = organizationBasePermissionType,
      projectPermissionType = projectPermission?.type,
      projectPermission?.languageIds
    )

    return ProjectPermissionData(
      project = project,
      organizationRole = organizationRole,
      organizationBasePermissions = organizationBasePermissionType,
      computedPermissions = computed,
      directPermissions = projectPermission
    )
  }

  fun getPermittedTranslateLanguagesForUserIds(userIds: List<Long>): Map<Long, List<Long>> {
    val data = permissionRepository.getUserPermittedLanguageIds(userIds)
    val result = mutableMapOf<Long, MutableList<Long>>()
    data.forEach {
      val languageIds = result.computeIfAbsent(it[0]) {
        mutableListOf()
      }
      languageIds.add(it[1])
    }
    return result
  }

  fun getPermittedTranslateLanguagesForProjectIds(projectIds: List<Long>, userId: Long): Map<Long, List<Long>> {
    val data = permissionRepository.getProjectPermittedLanguageIds(projectIds, userId)
    val result = mutableMapOf<Long, MutableList<Long>>()
    data.forEach {
      val languageIds = result.computeIfAbsent(it[0]) {
        mutableListOf()
      }
      languageIds.add(it[1])
    }
    return result
  }

  fun getProjectPermissionData(projectId: Long, userAccountId: Long): ProjectPermissionData {
    val project = projectService.findDto(projectId) ?: throw NotFoundException()
    return getProjectPermissionData(project, userAccountId)
  }

  fun create(permission: Permission): Permission {
    return cachedPermissionService.create(permission)
  }

  fun delete(permission: Permission) {
    return cachedPermissionService.delete(permission)
  }

  /**
   * Deletes all permissions in project
   * No need to evict cache, since this is only used when project is deleted
   */
  fun deleteAllByProject(projectId: Long) {
    val ids = permissionRepository.getIdsByProject(projectId)
    permissionRepository.deleteByIdIn(ids)
  }

  @Transactional
  fun grantFullAccessToProject(userAccount: UserAccount, project: Project) {
    val permission = Permission(
      type = ProjectPermissionType.MANAGE,
      project = project,
      user = userAccount
    )
    create(permission)
  }

  fun computeProjectPermissionType(
    organizationRole: OrganizationRoleType?,
    organizationBasePermissionType: ProjectPermissionType?,
    projectPermissionType: ProjectPermissionType?,
    projectPermissionLanguages: Set<Long>?
  ): ComputedPermissionDto {
    if (organizationRole == null) {
      return ComputedPermissionDto(projectPermissionType, projectPermissionLanguages)
    }

    if (organizationRole == OrganizationRoleType.OWNER) {
      return ComputedPermissionDto(ProjectPermissionType.MANAGE, null)
    }

    if (organizationRole == OrganizationRoleType.MEMBER) {
      if (projectPermissionType == null) {
        return ComputedPermissionDto(organizationBasePermissionType, null)
      }
      if (organizationBasePermissionType == null) {
        return ComputedPermissionDto(projectPermissionType, projectPermissionLanguages)
      }

      if (projectPermissionType.power > organizationBasePermissionType.power) {
        return ComputedPermissionDto(projectPermissionType, projectPermissionLanguages)
      }
    }
    return ComputedPermissionDto(organizationBasePermissionType, null)
  }

  fun createForInvitation(
    invitation: Invitation,
    project: Project,
    type: ProjectPermissionType,
    languages: Collection<Language>?
  ): Permission {
    return cachedPermissionService.createForInvitation(invitation, project, type, languages)
  }

  fun findOneByProjectIdAndUserId(projectId: Long, userId: Long): Permission? {
    return cachedPermissionService.findOneByProjectIdAndUserId(projectId, userId)
  }

  fun findOneDtoByProjectIdAndUserId(projectId: Long, userId: Long): PermissionDto? {
    return cachedPermissionService.findOneDtoByProjectIdAndUserId(projectId, userId)
  }

  fun acceptInvitation(permission: Permission, userAccount: UserAccount): Permission {
    return cachedPermissionService.acceptInvitation(permission, userAccount)
  }

  fun setUserDirectPermission(
    projectId: Long,
    userId: Long,
    newPermissionType: ProjectPermissionType,
    languages: Set<Language>? = null
  ): Permission? {
    validateLanguagePermissions(languages, newPermissionType)

    val data = this.getProjectPermissionData(projectId, userId)

    data.computedPermissions.type ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)

    data.organizationRole?.let {
      if (data.organizationRole == OrganizationRoleType.OWNER) {
        throw BadRequestException(Message.USER_IS_ORGANIZATION_OWNER)
      }

      if (data.organizationBasePermissions!!.power > newPermissionType.power) {
        throw BadRequestException(Message.CANNOT_SET_LOWER_THAN_ORGANIZATION_BASE_PERMISSIONS)
      }

      if (data.organizationBasePermissions == newPermissionType && data.directPermissions != null) {
        findById(data.directPermissions.id)?.let {
          delete(it)
        }
        return null
      }
    }

    val permission = data.directPermissions?.let { findById(it.id) } ?: let {
      val userAccount = userAccountService[userId].get()
      val project = projectService.get(data.project.id)
      Permission(user = userAccount, project = project, type = newPermissionType)
    }

    permission.type = newPermissionType
    permission.languages = languages?.toMutableSet() ?: mutableSetOf()
    return cachedPermissionService.save(permission)
  }

  fun onProjectTransferredToUser(project: Project, userAccount: UserAccount) {
    val permission = findOneByProjectIdAndUserId(project.id, userAccount.id)
      ?: Permission().also {
        it.user = userAccount
        it.project = project
      }
    permission.type = ProjectPermissionType.MANAGE
    permission.languages = mutableSetOf()
    cachedPermissionService.save(permission)
  }

  private fun validateLanguagePermissions(
    languages: Set<Language>?,
    newPermissionType: ProjectPermissionType
  ) {
    if (!languages.isNullOrEmpty() && newPermissionType != ProjectPermissionType.TRANSLATE) {
      throw BadRequestException(Message.ONLY_TRANSLATE_PERMISSION_ACCEPTS_LANGUAGES)
    }
  }

  fun saveAll(permissions: Iterable<Permission>) {
    cachedPermissionService.saveAll(permissions)
  }

  fun revoke(projectId: Long, userId: Long) {
    val data = this.getProjectPermissionData(projectId, userId)
    if (data.organizationRole != null) {
      throw BadRequestException(Message.USER_IS_ORGANIZATION_MEMBER)
    }

    data.directPermissions?.let {
      findById(it.id)?.let { found ->
        cachedPermissionService.delete(found)
      }
    } ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)
  }

  fun onLanguageDeleted(language: Language) {
    val permissions = permissionRepository.findAllByPermittedLanguage(language)
    permissions.forEach { permission ->
      val hasAccessOnlyToDeletedLanguage = permission.languages.size == 1 &&
        permission.languages.first().id == language.id

      if (hasAccessOnlyToDeletedLanguage) {
        permission.languages = mutableSetOf()
        permission.type = ProjectPermissionType.VIEW
        cachedPermissionService.save(permission)
        return@forEach
      }

      permission.languages.removeIf { it.id == language.id }
      cachedPermissionService.save(permission)
    }
  }
}
