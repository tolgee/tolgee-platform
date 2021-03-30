package io.tolgee.service

import io.tolgee.dtos.request.CreateOrganizationDto
import io.tolgee.model.Organization
import io.tolgee.repository.OrganizationRepository
import io.tolgee.security.AuthenticationFacade
import org.apache.commons.lang3.NotImplementedException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
open class OrganizationService(
        private val organizationRepository: OrganizationRepository,
        private val authenticationFacade: AuthenticationFacade,
) {

    open fun findPermittedPaged(pageable: Pageable): Page<Organization> {
        return organizationRepository.findAllPermitted(authenticationFacade.userAccount.id, pageable)
    }

    open fun get(id: Long): Organization? {
        return organizationRepository.getOne(id)
    }

    open fun get(addressPart: String): Organization? {
        return organizationRepository.getOneByAddressPart(addressPart)
    }

    open fun create(createOrganizationDto: CreateOrganizationDto): Organization {
        //return organizationRepository.getOne(createOrganizationDto)
        throw NotImplementedException()
    }


    open fun edit(editOrganizationDto: CreateOrganizationDto): Organization {
        //return organizationRepository.getOne()
        throw NotImplementedException()
    }

}
