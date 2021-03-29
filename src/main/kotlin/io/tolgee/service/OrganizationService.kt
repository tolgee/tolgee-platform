package io.tolgee.service

import io.tolgee.dtos.request.CreateOrganizationDto
import io.tolgee.model.Organization
import io.tolgee.repository.OrganizationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
open class OrganizationService(
        private val organizationRepository: OrganizationRepository
) {

    open fun findPaged(pageable: Pageable): Page<Organization> {
        return organizationRepository.findAll(pageable)
    }


    open fun get(id: Long): Organization {
        return organizationRepository.getOne(id)
    }


    open fun create(createOrganizationDto: CreateOrganizationDto): Organization {
        //return organizationRepository.getOne(createOrganizationDto)
    }


    open fun edit(editOrganizationDto: CreateOrganizationDto): Organization {
        //return organizationRepository.getOne()
    }

}
