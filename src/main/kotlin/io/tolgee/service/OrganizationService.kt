package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.OrganizationDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.repository.OrganizationRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.util.AddressPartGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class OrganizationService(
        private val organizationRepository: OrganizationRepository,
        private val authenticationFacade: AuthenticationFacade,
        private val addressPartGenerator: AddressPartGenerator,
        private val repositoryService: RepositoryService,
        private val organizationMemberRoleService: OrganizationMemberRoleService
) {

    @Transactional
    open fun create(createDto: OrganizationDto): Organization {
        return this.create(createDto, authenticationFacade.userAccount)
    }

    @Transactional
    open fun create(createDto: OrganizationDto, userAccount: UserAccount): Organization {
        if (createDto.addressPart != null && !validateUniqueAddressPart(createDto.addressPart!!)) {
            throw ValidationException(Message.ADDRESS_PART_NOT_UNIQUE)
        }

        val addressPart = createDto.addressPart
                ?: addressPartGenerator.generate(createDto.name!!, 3, 60) {
                    this.validateUniqueAddressPart(it)
                }

        Organization(
                name = createDto.name,
                description = createDto.description,
                addressPart = addressPart,
                basePermissions = createDto.basePermissions
        ).let {
            organizationRepository.save(it)
            organizationMemberRoleService.grantOwnerRoleToUser(userAccount, it)
            return it
        }
    }

    open fun findPermittedPaged(pageable: Pageable): Page<Organization> {
        return organizationRepository.findAllPermitted(authenticationFacade.userAccount.id, pageable)
    }

    open fun get(id: Long): Organization? {
        return organizationRepository.findByIdOrNull(id)
    }

    open fun get(addressPart: String): Organization? {
        return organizationRepository.getOneByAddressPart(addressPart)
    }

    open fun edit(id: Long, editDto: OrganizationDto): Organization {
        val organization = this.get(id) ?: throw NotFoundException()

        if (editDto.addressPart == null) {
            editDto.addressPart = organization.addressPart
        }

        if (editDto.addressPart != organization.addressPart && !validateUniqueAddressPart(editDto.addressPart!!)) {
            throw ValidationException(Message.ADDRESS_PART_NOT_UNIQUE)
        }

        organization.name = editDto.name
        organization.description = editDto.description
        organization.addressPart = editDto.addressPart
        organization.basePermissions = editDto.basePermissions
        organizationRepository.save(organization)
        return organization
    }

    @Transactional
    open fun delete(id: Long) {
        val organization = this.get(id) ?: throw NotFoundException()

        repositoryService.findAllInOrganization(id).forEach {
            repositoryService.deleteRepository(it.id)
        }

        organization.memberRoles.forEach {
            organizationMemberRoleService.delete(it.id!!)
        }

        this.organizationRepository.delete(organization)
    }

    /**
     * Checks address part uniqueness
     * @return Returns true if valid
     */
    open fun validateUniqueAddressPart(addressPart: String): Boolean {
        return organizationRepository.countAllByAddressPart(addressPart) < 1
    }
}
