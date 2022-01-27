@file:Suppress("SpringElInspection")

package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.dtos.cacheable.PermissionDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
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
    return getProjectPermissionData(projectId, userAccountId).computedPermissions
  }

  fun getProjectPermissionData(projectId: Long, userAccountId: Long): ProjectPermissionData {
    val project = projectService.findDto(projectId) ?: throw NotFoundException()
    val projectPermission = findOneDtoByProjectIdAndUserId(projectId, userAccountId)

    val organizationRole = project.organizationOwnerId
      ?.let { organizationRoleService.findType(userAccountId, it) }

    val organizationBasePermissionType = project.organizationOwnerId?.let {
      organizationService.find(it)?.basePermissions ?: throw NotFoundException()
    }

    val computed = computeProjectPermissionType(
      organizationRole = organizationRole,
      organizationBasePermissionType = organizationBasePermissionType,
      projectPermissionType = projectPermission?.type
    )

    return ProjectPermissionData(
      project = project,
      organizationRole = organizationRole,
      organizationBasePermissions = organizationBasePermissionType,
      computedPermissions = computed,
      directPermissions = projectPermission
    )
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
    projectPermissionType: ProjectPermissionType?
  ): ProjectPermissionType? {
    if (organizationRole == null) {
      return projectPermissionType
    }

    if (organizationRole == OrganizationRoleType.OWNER) {
      return ProjectPermissionType.MANAGE
    }

    if (organizationRole == OrganizationRoleType.MEMBER) {
      if (projectPermissionType == null) {
        return organizationBasePermissionType
      }
      if (organizationBasePermissionType == null) {
        return projectPermissionType
      }

      if (projectPermissionType.power > organizationBasePermissionType.power) {
        return projectPermissionType
      }
    }
    return organizationBasePermissionType
  }

  fun createForInvitation(invitation: Invitation, project: Project, type: ProjectPermissionType): Permission {
    return cachedPermissionService.createForInvitation(invitation, project, type)
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
    ignoreUserOrganizationOwner: Boolean = false
  ): Permission? {
    val data = this.getProjectPermissionData(projectId, userId)

    data.computedPermissions ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)

    data.organizationRole?.let {
      if (data.organizationRole == OrganizationRoleType.OWNER && !ignoreUserOrganizationOwner) {
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
    return cachedPermissionService.save(permission)
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
}
