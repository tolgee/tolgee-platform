package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.SetOrganizationRoleDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
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
        private val authenticationFacade: AuthenticationFacade,
        private val userAccountService: UserAccountService
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
        val role = organizationMemberRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)
        if (role != null) {
            return true
        }
        return false
    }

    open fun isUserOwner(userId: Long, organizationId: Long): Boolean {
        val role = organizationMemberRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)
        if (role?.type == OrganizationRoleType.OWNER) {
            return true
        }
        return false
    }

    open fun getType(userId: Long, organizationId: Long): OrganizationRoleType? {
        organizationMemberRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)
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

    open fun leave(organizationId: Long) {
        this.removeUser(organizationId, authenticationFacade.userAccount.id!!)
    }

    open fun removeUser(organizationId: Long, userId: Long) {
        organizationMemberRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)?.let {
            organizationMemberRoleRepository.delete(it)
        }
    }

    open fun delete(id: Long) {
        organizationMemberRoleRepository.deleteById(id)
    }

    open fun grantMemberRoleToUser(user: UserAccount, organization: Organization) {
        this.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.MEMBER)
    }

    open fun grantOwnerRoleToUser(user: UserAccount, organization: Organization) {
        this.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.OWNER)
    }

    fun setMemberRole(organizationId: Long, userId: Long, dto: SetOrganizationRoleDto) {
        val user = userAccountService.get(userId).orElseThrow { NotFoundException() }!!
        organizationMemberRoleRepository.findOneByUserIdAndOrganizationId(user.id!!, organizationId)?.let {
            it.type = dto.roleType
            organizationMemberRoleRepository.save(it)
        } ?: throw ValidationException(Message.USER_IS_NOT_MEMBER_OF_ORGANIZATION)
    }
}
