package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.RepositoryPermissionData
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Invitation
import io.tolgee.model.Permission
import io.tolgee.model.Permission.Companion.builder
import io.tolgee.model.Permission.RepositoryPermissionType
import io.tolgee.model.Repository
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

    open fun getAllOfRepository(repository: Repository?): Set<Permission> {
        return permissionRepository.getAllByRepositoryAndUserNotNull(repository)
    }

    open fun findById(id: Long): Permission? {
        return permissionRepository.findById(id).orElse(null)
    }

    open fun getRepositoryPermissionType(repositoryId: Long, userAccount: UserAccount) = getRepositoryPermissionType(repositoryId, userAccount.id!!)

    open fun getRepositoryPermissionType(repositoryId: Long, userAccountId: Long): RepositoryPermissionType? {
        return getRepositoryPermissionData(repositoryId, userAccountId).computedPermissions
    }

    open fun getRepositoryPermissionData(repositoryId: Long, userAccountId: Long): RepositoryPermissionData {
        val repository = repositoryService.get(repositoryId).orElseThrow { NotFoundException() }!!
        val repositoryPermission = permissionRepository.findOneByRepositoryIdAndUserId(repositoryId, userAccountId)

        val organization = repository.organizationOwner
        val organizationRole = organization?.let { organizationRoleService.getType(userAccountId, organization.id!!) }
        val organizationBasePermissionType = organization?.basePermissions
        val computed = computeRepositoryPermissionType(organizationRole, organizationBasePermissionType, repositoryPermission?.type)

        return RepositoryPermissionData(
                repository = repository,
                organization = organization,
                organizationRole = organizationRole,
                organizationBasePermissions = organizationBasePermissionType,
                computedPermissions = computed,
                directPermissions = repositoryPermission
        )
    }

    open fun create(permission: Permission) {
        permission.repository!!.permissions.add(permission)
        permissionRepository.save(permission)
    }

    open fun delete(permission: Permission) {
        permissionRepository.delete(permission)
    }

    open fun deleteAllByRepository(repositoryId: Long?) {
        permissionRepository.deleteAllByRepositoryId(repositoryId)
    }

    @Transactional
    open fun grantFullAccessToRepo(userAccount: UserAccount?, repository: Repository?) {
        val permission = builder().type(RepositoryPermissionType.MANAGE).repository(repository).user(userAccount).build()
        create(permission)
    }

    @Transactional
    open fun editPermission(permission: Permission, type: RepositoryPermissionType?) {
        permission.type = type
        permissionRepository.save(permission)
    }

    open fun computeRepositoryPermissionType(
            organizationRole: OrganizationRoleType?,
            organizationBasePermissionType: RepositoryPermissionType?,
            repositoryPermissionType: RepositoryPermissionType?
    ): RepositoryPermissionType? {
        if (organizationRole == null) {
            return repositoryPermissionType
        }

        if (organizationRole == OrganizationRoleType.OWNER) {
            return RepositoryPermissionType.MANAGE
        }

        if (organizationRole == OrganizationRoleType.MEMBER) {
            if (repositoryPermissionType == null) {
                return organizationBasePermissionType
            }
            if (organizationBasePermissionType == null) {
                return repositoryPermissionType
            }

            if (repositoryPermissionType.power > organizationBasePermissionType.power) {
                return repositoryPermissionType
            }
        }
        return organizationBasePermissionType
    }

    open fun createForInvitation(invitation: Invitation, repository: Repository, type: RepositoryPermissionType): Permission {
        return Permission(invitation = invitation, repository = repository, type = type).let {
            permissionRepository.save(it)
        }
    }

    open fun findOneByRepositoryIdAndUserId(repositoryId: Long, userId: Long): Permission? {
        return permissionRepository.findOneByRepositoryIdAndUserId(repositoryId, userId)
    }

    open fun acceptInvitation(permission: Permission, userAccount: UserAccount) {
        permission.invitation = null
        permission.user = userAccount
        permissionRepository.save(permission)
    }

    open fun setUserDirectPermission(repositoryId: Long, userId: Long, newPermissionType: RepositoryPermissionType) {
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
            Permission(user = userAccount, repository = data.repository, type = newPermissionType)
        }

        permission.type = newPermissionType
        permissionRepository.save(permission)
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
