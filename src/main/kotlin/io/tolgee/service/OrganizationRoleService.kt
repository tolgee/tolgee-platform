package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.SetOrganizationRoleDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Invitation
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.repository.OrganizationRoleRepository
import io.tolgee.security.AuthenticationFacade
import org.springframework.stereotype.Service

@Service
open class OrganizationRoleService(
        private val organizationRoleRepository: OrganizationRoleRepository,
        private val authenticationFacade: AuthenticationFacade,
        private val userAccountService: UserAccountService
) {

    open fun checkUserIsOwner(userId: Long, organizationId: Long) {
        if (this.isUserOwner(userId, organizationId)) return else throw PermissionException()
    }

    open fun checkUserIsOwner(organizationId: Long) {
        this.checkUserIsOwner(authenticationFacade.userAccount.id!!, organizationId)
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
        val role = organizationRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)
        if (role != null) {
            return true
        }
        return false
    }

    open fun isUserOwner(userId: Long, organizationId: Long): Boolean {
        val role = organizationRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)
        if (role?.type == OrganizationRoleType.OWNER) {
            return true
        }
        return false
    }

    open fun getType(userId: Long, organizationId: Long): OrganizationRoleType? {
        organizationRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)
                ?.let { return it.type }
        return null
    }

    open fun grantRoleToUser(user: UserAccount, organization: Organization, organizationRoleType: OrganizationRoleType) {
        OrganizationRole(user = user, organization = organization, type = organizationRoleType)
                .let {
                    organizationRoleRepository.save(it)
                    organization.memberRoles.add(it)
                    user.organizationRoles.add(it)
                }
    }

    open fun leave(organizationId: Long) {
        this.removeUser(organizationId, authenticationFacade.userAccount.id!!)
    }

    open fun removeUser(organizationId: Long, userId: Long) {
        organizationRoleRepository.findOneByUserIdAndOrganizationId(userId, organizationId)?.let {
            organizationRoleRepository.delete(it)
        }
    }

    open fun delete(id: Long) {
        organizationRoleRepository.deleteById(id)
    }

    open fun grantMemberRoleToUser(user: UserAccount, organization: Organization) {
        this.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.MEMBER)
    }

    open fun grantOwnerRoleToUser(user: UserAccount, organization: Organization) {
        this.grantRoleToUser(user, organization, organizationRoleType = OrganizationRoleType.OWNER)
    }

    fun setMemberRole(organizationId: Long, userId: Long, dto: SetOrganizationRoleDto) {
        val user = userAccountService.get(userId).orElseThrow { NotFoundException() }!!
        organizationRoleRepository.findOneByUserIdAndOrganizationId(user.id!!, organizationId)?.let {
            it.type = dto.roleType
            organizationRoleRepository.save(it)
        } ?: throw ValidationException(Message.USER_IS_NOT_MEMBER_OF_ORGANIZATION)
    }

    fun createForInvitation(invitation: Invitation, type: OrganizationRoleType, organization: Organization): OrganizationRole {
        return OrganizationRole(invitation = invitation, type = type, organization = organization).let {
            organizationRoleRepository.save(it)
        }
    }

    fun acceptInvitation(organizationRole: OrganizationRole, userAccount: UserAccount) {
        organizationRole.invitation = null
        organizationRole.user = userAccount
        organizationRoleRepository.save(organizationRole)
    }
}
