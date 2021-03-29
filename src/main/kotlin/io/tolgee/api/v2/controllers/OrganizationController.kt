/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.model.Organization
import io.tolgee.repository.OrganizationRepository
import io.tolgee.service.OrganizationService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/repository/organisations", "/api/repository/{repositoryId:[0-9]+}/organisations"])
open class OrganizationController(
        private val organizationService: OrganizationService,
        private val organizationRepository: OrganizationRepository,
        private val pagedResourcesAssembler: PagedResourcesAssembler<Organization>,
        private val organizationModelAssembler: OrganizationModelAssembler
) {

    @PostMapping
    open fun create() {

    }

    @GetMapping("/{id:[0-9]+}")
    open fun get(@PathVariable("id") id: Long): Organization? {
        return null
    }

    @GetMapping("")
    open fun getAllOrganizations(pageable: Pageable): PagedModel<OrganizationModel> {
        val organizations = organizationRepository.findAll(pageable)
        return pagedResourcesAssembler.toModel(organizations, organizationModelAssembler)
    }
}
