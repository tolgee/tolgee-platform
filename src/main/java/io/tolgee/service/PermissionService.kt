package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.RepositoryPermissionData
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
open class PermissionService @Autowired constructor(private val permissionRepository: PermissionRepository,
                                                    private val organizationRoleService: OrganizationRoleService,
                                                    private val userAccountService: UserAccountService
) {

    @set:Autowired
    lateinit var repositoryService: RepositoryService

    open fun getAllOfRepository(project: Project?): Set<Permission> {
        return permissionRepository.getAllByProjectAndUserNotNull(project)
    }

    open fun findById(id: Long): Permission? {
        return permissionRepository.findById(id).orElse(null)
    }

    open fun getRepositoryPermissionType(repositoryId: Long, userAccount: UserAccount) = getRepositoryPermissionType(repositoryId, userAccount.id!!)

    open fun getRepositoryPermissionType(repositoryId: Long, userAccountId: Long): ProjectPermissionType? {
        return getRepositoryPermissionData(repositoryId, userAccountId).computedPermissions
    }

    open fun getRepositoryPermissionData(repositoryId: Long, userAccountId: Long): RepositoryPermissionData {
        val repository = repositoryService.get(repositoryId).orElseThrow { NotFoundException() }!!
        val repositoryPermission = permissionRepository.findOneByProjectIdAndUserId(repositoryId, userAccountId)

        val organization = repository.organizationOwner
        val organizationRole = organization?.let { organizationRoleService.getType(userAccountId, organization.id!!) }
        val organizationBasePermissionType = organization?.basePermissions
        val computed = computeRepositoryPermissionType(organizationRole, organizationBasePermissionType, repositoryPermission?.type)

        return RepositoryPermissionData(
                project = repository,
                organization = organization,
                organizationRole = organizationRole,
                organizationBasePermissions = organizationBasePermissionType,
                computedPermissions = computed,
                directPermissions = repositoryPermission
        )
    }

    open fun create(permission: Permission) {
        permission.project!!.permissions.add(permission)
        permissionRepository.save(permission)
    }

    open fun delete(permission: Permission) {
        permissionRepository.delete(permission)
    }

    open fun deleteAllByRepository(repositoryId: Long?) {
        permissionRepository.deleteAllByProjectId(repositoryId)
    }

    @Transactional
    open fun grantFullAccessToRepo(userAccount: UserAccount?, project: Project?) {
        val permission = builder().type(ProjectPermissionType.MANAGE).project(project).user(userAccount).build()
        create(permission)
    }

    @Transactional
    open fun editPermission(permission: Permission, type: ProjectPermissionType?) {
        permission.type = type
        permissionRepository.save(permission)
    }

    open fun computeRepositoryPermissionType(
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

    open fun createForInvitation(invitation: Invitation, project: Project, type: ProjectPermissionType): Permission {
        return Permission(invitation = invitation, project = project, type = type).let {
            permissionRepository.save(it)
        }
    }

    open fun findOneByRepositoryIdAndUserId(repositoryId: Long, userId: Long): Permission? {
        return permissionRepository.findOneByProjectIdAndUserId(repositoryId, userId)
    }

    open fun acceptInvitation(permission: Permission, userAccount: UserAccount) {
        permission.invitation = null
        permission.user = userAccount
        permissionRepository.save(permission)
    }

    open fun setUserDirectPermission(repositoryId: Long, userId: Long, newPermissionType: ProjectPermissionType) {
        val data = this.getRepositoryPermissionData(repositoryId, userId)

        data.computedPermissions ?: throw BadRequestException(Message.USER_HAS_NO_REPOSITORY_ACCESS)

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

    open fun revoke(repositoryId: Long, userId: Long) {
        val data = this.getRepositoryPermissionData(repositoryId, userId)
        if (data.organizationRole != null) {
            throw BadRequestException(Message.USER_IS_ORGANIZATION_MEMBER)
        }

        data.directPermissions?.let {
            permissionRepository.delete(it)
        } ?: throw BadRequestException(Message.USER_HAS_NO_REPOSITORY_ACCESS)
    }
}
