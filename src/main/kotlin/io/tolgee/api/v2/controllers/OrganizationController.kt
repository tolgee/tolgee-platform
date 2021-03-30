/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.api.v2.hateoas.organization.UserAccountWithOrganizationRoleModel
import io.tolgee.api.v2.hateoas.organization.UserAccountWithOrganizationRoleModelAssembler
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.CreateOrganizationDto
import io.tolgee.dtos.request.EditOrganizationDto
import io.tolgee.dtos.request.SetUserPermissionsDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.repository.OrganizationRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.OrganizationMemberRoleService
import io.tolgee.service.OrganizationService
import io.tolgee.service.UserAccountService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/organizations", "/v2/organizations"])
open class OrganizationController(
        private val organizationService: OrganizationService,
        private val organizationRepository: OrganizationRepository,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val pagedResourcesAssembler: PagedResourcesAssembler<Organization>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val userPagedResourcesAssembler: PagedResourcesAssembler<Array<Any>>,
        private val organizationModelAssembler: OrganizationModelAssembler,
        private val userAccountWithOrganizationRoleModelAssembler: UserAccountWithOrganizationRoleModelAssembler,
        private val tolgeeProperties: TolgeeProperties,
        private val authenticationFacade: AuthenticationFacade,
        private val organizationMemberRoleService: OrganizationMemberRoleService,
        private val userAccountService: UserAccountService
) {

    @PostMapping
    open fun create(@RequestBody @Valid dto: CreateOrganizationDto) {
        if (!this.tolgeeProperties.authentication.userCanCreateOrganizations
                && authenticationFacade.userAccount.role != UserAccount.Role.ADMIN) {
            throw PermissionException()
        }
    }

    @GetMapping("/{id:[0-9]+}")
    open fun get(@PathVariable("id") id: Long): OrganizationModel? {
        organizationService.get(id)?.let {
            organizationMemberRoleService.checkUserIsMemberOrOwner(id)
            return organizationModelAssembler.toModel(it)
        }
                ?: throw NotFoundException()
    }

    @GetMapping("/{addressPart:.*[a-z].*}")
    open fun get(@PathVariable("addressPart") addressPart: String): OrganizationModel {
        organizationService.get(addressPart)?.let {
            organizationMemberRoleService.checkUserIsMemberOrOwner(it.id!!)
            return organizationModelAssembler.toModel(it)
        }
                ?: throw NotFoundException()
    }

    @GetMapping("")
    open fun getAll(pageable: Pageable): PagedModel<OrganizationModel> {
        val organizations = organizationRepository.findAll(pageable)
        return pagedResourcesAssembler.toModel(organizations, organizationModelAssembler)
    }

    @PutMapping
    open fun update(@RequestBody @Valid dto: EditOrganizationDto) {

    }

    @DeleteMapping("/{id:[0-9]+}/leave")
    open fun delete(@PathVariable("id") id: Long) {

    }

    @GetMapping("/{id:[0-9]+}/users")
    open fun getAllUsers(
            @PathVariable("id") id: Long,
            pageable: Pageable,
            @RequestParam("search") search: String?): PagedModel<UserAccountWithOrganizationRoleModel> {
        organizationMemberRoleService.checkUserIsMemberOrOwner(id)
        val allInOrganization = userAccountService.getAllInOrganization(id, pageable, search)
        return userPagedResourcesAssembler.toModel(allInOrganization, userAccountWithOrganizationRoleModelAssembler)
    }

    @PutMapping("/{id:[0-9]+}/leave")
    open fun leaveOrganization(@PathVariable("id") id: Long) {

    }

    @PutMapping("/{id:[0-9]+}/set-user-permissions")
    open fun setUserRole(@PathVariable("id") id: Long, @RequestBody dto: SetUserPermissionsDto) {

    }

    @DeleteMapping("/{organizationId:[0-9]+}/users/{userId:[0-9]+}")
    open fun removeUser(@PathVariable("organizationId") organizationId: Long, @PathVariable("userId") userId: Long) {

    }

}
