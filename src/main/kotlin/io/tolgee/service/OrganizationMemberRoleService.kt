package io.tolgee.service

import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationMemberRole
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.repository.OrganizationMemberRoleRepository
import io.tolgee.security.AuthenticationFacade
import org.springframework.stereotype.Service

@Service
open class OrganizationMemberRoleService(
        private val organizationMemberRoleRepository: OrganizationMemberRoleRepository,
        private val authenticationFacade: AuthenticationFacade
) {

    open fun checkUserIsOwner(userId: Long, organizationId: Long) {
        if (this.isUserOwner(userId, organizationId)) return else throw PermissionException()
    }

    open fun checkUserIsOwner(organizationId: Long) {
        if (this.isUserOwner(authenticationFacade.userAccount.id!!, organizationId)) {
            return
        }
        throw PermissionException()
    }

    open fun checkUserIsMemberOrOwner(userId: Long, organizationId: Long) {
        if (isUserMemberOrOwner(userId, organizationId)) {
            return
        }
        throw PermissionException()
    }

    open fun checkUserIsMemberOrOwner(organizationId: Long) {
        this.checkUserIsMemberOrOwner(this.authenticationFacade.userAccount.id!!, organizationId)
    }

    open fun isUserMemberOrOwner(userId: Long, organizationId: Long): Boolean {
        val role = organizationMemberRoleRepository.findAllByUserIdAndOrganizationId(userId, organizationId)
        if (role != null) {
            return true
        }
        return false
    }

    open fun isUserOwner(userId: Long, organizationId: Long): Boolean {
        val role = organizationMemberRoleRepository.findAllByUserIdAndOrganizationId(userId, organizationId)
        if (role?.type == OrganizationRoleType.OWNER) {
            return true
        }
        return false
    }

    open fun getType(userId: Long, organizationId: Long): OrganizationRoleType? {
        organizationMemberRoleRepository.findAllByUserIdAndOrganizationId(userId, organizationId)
                ?.let { return it.type }
        return null
    }

    open fun grantRoleToUser(user: UserAccount, organization: Organization, organizationRoleType: OrganizationRoleType) {
        OrganizationMemberRole(user = user, organization = organization, type = organizationRoleType)
                .let {
                    organizationMemberRoleRepository.save(it)
                    organization.memberRoles.add(it)
                    user.organizationMemberRoles.add(it)
                }
    }

    open fun grantMemberRoleToUser(user: UserAccount, organization: Organization) {
        this.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.MEMBER)
    }

    open fun grantOwnerRoleToUser(user: UserAccount, organization: Organization) {
        this.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.OWNER)
    }
}
