package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.ProjectPermissionData
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.model.Permission.Companion.builder
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.repository.PermissionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PermissionService @Autowired constructor(
  private val permissionRepository: PermissionRepository,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService
) {

  @set:Autowired
  lateinit var projectService: ProjectService

  fun getAllOfProject(project: Project?): Set<Permission> {
    return permissionRepository.getAllByProjectAndUserNotNull(project)
  }

  fun findById(id: Long): Permission? {
    return permissionRepository.findById(id).orElse(null)
  }

  fun getProjectPermissionType(projectId: Long, userAccount: UserAccount) =
    getProjectPermissionType(projectId, userAccount.id!!)

  fun getProjectPermissionType(projectId: Long, userAccountId: Long): ProjectPermissionType? {
    return getProjectPermissionData(projectId, userAccountId).computedPermissions
  }

  fun getProjectPermissionData(projectId: Long, userAccountId: Long): ProjectPermissionData {
    val project = projectService.get(projectId).orElseThrow { NotFoundException() }!!
    val projectPermission = permissionRepository.findOneByProjectIdAndUserId(projectId, userAccountId)

    val organization = project.organizationOwner
    val organizationRole = organization?.let { organizationRoleService.getType(userAccountId, organization.id!!) }
    val organizationBasePermissionType = organization?.basePermissions
    val computed = computeProjectPermissionType(
      organizationRole = organizationRole,
      organizationBasePermissionType = organizationBasePermissionType,
      projectPermissionType = projectPermission?.type
    )

    return ProjectPermissionData(
      project = project,
      organization = organization,
      organizationRole = organizationRole,
      organizationBasePermissions = organizationBasePermissionType,
      computedPermissions = computed,
      directPermissions = projectPermission
    )
  }

  fun create(permission: Permission) {
    permission.project!!.permissions.add(permission)
    permissionRepository.save(permission)
  }

  fun delete(permission: Permission) {
    permissionRepository.delete(permission)
  }

  fun deleteAllByProject(projectId: Long) {
    val ids = permissionRepository.getIdsByProject(projectId)
    permissionRepository.deleteByIdIn(ids)
  }

  @Transactional
  fun grantFullAccessToRepo(userAccount: UserAccount?, project: Project?) {
    val permission = builder().type(ProjectPermissionType.MANAGE).project(project).user(userAccount).build()
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
    return Permission(invitation = invitation, project = project, type = type).let {
      permissionRepository.save(it)
    }
  }

  fun findOneByProjectIdAndUserId(projectId: Long, userId: Long): Permission? {
    return permissionRepository.findOneByProjectIdAndUserId(projectId, userId)
  }

  fun acceptInvitation(permission: Permission, userAccount: UserAccount) {
    permission.invitation = null
    permission.user = userAccount
    permissionRepository.save(permission)
  }

  fun setUserDirectPermission(
    projectId: Long,
    userId: Long,
    newPermissionType: ProjectPermissionType
  ) {
    val data = this.getProjectPermissionData(projectId, userId)

    data.computedPermissions ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)

    data.organizationRole?.let {
      if (data.organizationRole == OrganizationRoleType.OWNER) {
        throw BadRequestException(Message.USER_IS_ORGANIZATION_OWNER)
      }

      if (data.organizationBasePermissions!!.power > newPermissionType.power) {
        throw BadRequestException(Message.CANNOT_SET_LOWER_THAN_ORGANIZATION_BASE_PERMISSIONS)
      }

      if (data.organizationBasePermissions == newPermissionType && data.directPermissions != null) {
        permissionRepository.delete(data.directPermissions)
        return
      }
    }

    val permission = data.directPermissions ?: let {
      val userAccount = userAccountService[userId].get()
      Permission(user = userAccount, project = data.project, type = newPermissionType)
    }

    permission.type = newPermissionType
    permissionRepository.save(permission)
  }

  fun saveAll(permissions: Iterable<Permission>) {
    this.permissionRepository.saveAll(permissions)
  }

  fun revoke(projectId: Long, userId: Long) {
    val data = this.getProjectPermissionData(projectId, userId)
    if (data.organizationRole != null) {
      throw BadRequestException(Message.USER_IS_ORGANIZATION_MEMBER)
    }

    data.directPermissions?.let {
      permissionRepository.delete(it)
    } ?: throw BadRequestException(Message.USER_HAS_NO_PROJECT_ACCESS)
  }
}
