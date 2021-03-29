package io.tolgee.service

import io.tolgee.exceptions.NotFoundException
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
                                                    private val organizationMemberRoleService: OrganizationMemberRoleService
) {

    @set:Autowired
    lateinit var repositoryService: RepositoryService

    open fun getAllOfRepository(repository: Repository?): Set<Permission> {
        return permissionRepository.getAllByRepositoryAndUserNotNull(repository)
    }

    open fun findById(id: Long): Permission? {
        return permissionRepository.findById(id).orElse(null)
    }

    open fun getRepositoryPermissionType(repositoryId: Long, userAccount: UserAccount): RepositoryPermissionType? {
        val repository = repositoryService.get(repositoryId).orElseThrow { NotFoundException() }
        val repositoryPermission = permissionRepository.findOneByRepositoryIdAndUserId(repositoryId, userAccount.id)
        repository.organizationOwner?.let { organization ->
            val type = organizationMemberRoleService.getType(userAccount.id!!, organization.id!!)
            if (type == OrganizationRoleType.OWNER) {
                return RepositoryPermissionType.MANAGE
            }

            if (type == OrganizationRoleType.MEMBER && (repositoryPermission == null ||
                            organization.basePermissions.power > repositoryPermission.type!!.power)) {
                return organization.basePermissions
            }
        }
        return repositoryPermission?.type
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
}
