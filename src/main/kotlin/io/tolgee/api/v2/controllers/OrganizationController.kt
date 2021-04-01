/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.api.v2.hateoas.invitation.OrganizationInvitationModel
import io.tolgee.api.v2.hateoas.invitation.OrganizationInvitationModelAssembler
import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.api.v2.hateoas.organization.UserAccountWithOrganizationRoleModel
import io.tolgee.api.v2.hateoas.organization.UserAccountWithOrganizationRoleModelAssembler
import io.tolgee.api.v2.hateoas.repository.RepositoryModel
import io.tolgee.api.v2.hateoas.repository.RepositoryModelAssembler
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.OrganizationDto
import io.tolgee.dtos.request.OrganizationInviteUserDto
import io.tolgee.dtos.request.SetOrganizationRoleDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.*
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.*
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/api/organizations", "/v2/organizations"])
open class OrganizationController(
        private val organizationService: OrganizationService,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val pagedResourcesAssembler: PagedResourcesAssembler<Organization>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val pagedRepositoryResourcesAssembler: PagedResourcesAssembler<Repository>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val userPagedResourcesAssembler: PagedResourcesAssembler<Array<Any>>,
        private val organizationModelAssembler: OrganizationModelAssembler,
        private val userAccountWithOrganizationRoleModelAssembler: UserAccountWithOrganizationRoleModelAssembler,
        private val tolgeeProperties: TolgeeProperties,
        private val authenticationFacade: AuthenticationFacade,
        private val organizationRoleService: OrganizationRoleService,
        private val userAccountService: UserAccountService,
        private val repositoryService: RepositoryService,
        private val repositoryModelAssembler: RepositoryModelAssembler,
        private val invitationService: InvitationService,
        private val organizationInvitationModelAssembler: OrganizationInvitationModelAssembler
) {

    @PostMapping
    open fun create(@RequestBody @Valid dto: OrganizationDto): ResponseEntity<OrganizationModel> {
        if (!this.tolgeeProperties.authentication.userCanCreateOrganizations
                && authenticationFacade.userAccount.role != UserAccount.Role.ADMIN) {
            throw PermissionException()
        }
        this.organizationService.create(dto).let {
            return ResponseEntity(organizationModelAssembler.toModel(it), HttpStatus.CREATED)
        }
    }

    @GetMapping("/{id:[0-9]+}")
    open fun get(@PathVariable("id") id: Long): OrganizationModel? {
        organizationService.get(id)?.let {
            organizationRoleService.checkUserIsMemberOrOwner(id)
            return it.toModel()
        }
                ?: throw NotFoundException()
    }

    @GetMapping("/{addressPart:.*[a-z].*}")
    open fun get(@PathVariable("addressPart") addressPart: String): OrganizationModel {
        organizationService.get(addressPart)?.let {
            organizationRoleService.checkUserIsMemberOrOwner(it.id!!)
            return it.toModel()
        }
                ?: throw NotFoundException()
    }

    @GetMapping("")
    open fun getAll(pageable: Pageable): PagedModel<OrganizationModel> {
        val organizations = organizationService.findPermittedPaged(pageable)
        return pagedResourcesAssembler.toModel(organizations, organizationModelAssembler)
    }

    @PutMapping("/{id:[0-9]+}")
    open fun update(@PathVariable("id") id: Long, @RequestBody @Valid dto: OrganizationDto): OrganizationModel {
        organizationRoleService.checkUserIsOwner(id)
        return this.organizationService.edit(id, editDto = dto).toModel()
    }

    @DeleteMapping("/{id:[0-9]+}")
    open fun delete(@PathVariable("id") id: Long) {
        organizationRoleService.checkUserIsOwner(id)
        organizationService.delete(id)
    }

    @GetMapping("/{id:[0-9]+}/users")
    open fun getAllUsers(
            @PathVariable("id") id: Long,
            pageable: Pageable,
            @RequestParam("search") search: String?): PagedModel<UserAccountWithOrganizationRoleModel> {
        organizationRoleService.checkUserIsMemberOrOwner(id)
        val allInOrganization = userAccountService.getAllInOrganization(id, pageable, search)
        return userPagedResourcesAssembler.toModel(allInOrganization, userAccountWithOrganizationRoleModelAssembler)
    }

    @PutMapping("/{id:[0-9]+}/leave")
    open fun leaveOrganization(@PathVariable("id") id: Long) {
        organizationRoleService.checkUserIsMemberOrOwner(id)
        organizationRoleService.leave(id)
    }

    @PutMapping("/{organizationId:[0-9]+}/users/{userId:[0-9]+}/set-role")
    open fun setUserRole(@PathVariable("organizationId") organizationId: Long,
                         @PathVariable("userId") userId: Long,
                         @RequestBody dto: SetOrganizationRoleDto
    ) {
        organizationRoleService.checkUserIsOwner(organizationId)
        organizationRoleService.setMemberRole(organizationId, userId, dto)
    }

    @DeleteMapping("/{organizationId:[0-9]+}/users/{userId:[0-9]+}")
    open fun removeUser(
            @PathVariable("organizationId") organizationId: Long,
            @PathVariable("userId") userId: Long
    ) {
        organizationRoleService.checkUserIsOwner(organizationId)
        organizationRoleService.removeUser(organizationId, userId)
    }

    @GetMapping("/{id:[0-9]+]}/repositories")
    open fun getAllRepositories(
            @PathVariable("id") id: Long,
            pageable: Pageable
    ): PagedModel<RepositoryModel> {
        return organizationService.get(id)?.let {
            organizationRoleService.checkUserIsMemberOrOwner(it.id!!)
            repositoryService.findAllInOrganization(it.id!!, pageable).let { repositories ->
                pagedRepositoryResourcesAssembler.toModel(repositories, repositoryModelAssembler)
            }
        } ?: throw NotFoundException()
    }

    @GetMapping("/{addressPart:.*[a-z].*}/repositories")
    open fun getAllRepositories(
            @PathVariable("addressPart") addressPart: String,
            pageable: Pageable
    ): PagedModel<RepositoryModel> {
        return organizationService.get(addressPart)?.let {
            organizationRoleService.checkUserIsMemberOrOwner(it.id!!)
            repositoryService.findAllInOrganization(it.id!!, pageable).let { repositories ->
                pagedRepositoryResourcesAssembler.toModel(repositories, repositoryModelAssembler)
            }
        } ?: throw NotFoundException()
    }

    @PutMapping("/{id:[0-9]+}/invite")
    @Operation(summary = "Generates user invitation link for organization")
    open fun inviteUser(
            @RequestBody @Valid invitation: OrganizationInviteUserDto,
            @PathVariable("id") id: Long
    ): OrganizationInvitationModel {
        organizationRoleService.checkUserIsOwner(id)

        return organizationService.get(id)?.let { organization ->
            invitationService.create(organization, invitation.type).let {
                organizationInvitationModelAssembler.toModel(it)
            }
        } ?: throw NotFoundException()
    }

    private fun Organization.toModel(): OrganizationModel {
        return this@OrganizationController.organizationModelAssembler.toModel(this)
    }

    @GetMapping("/{organizationId}/invitations")
    @Operation(summary = "Prints all invitations to organization")
    fun getInvitations(@PathVariable("organizationId") id: Long): CollectionModel<OrganizationInvitationModel> {
        val organization = organizationService.get(id) ?: throw NotFoundException()
        organizationRoleService.checkUserIsOwner(id)
        return invitationService.getForOrganization(organization).let {
            organizationInvitationModelAssembler.toCollectionModel(it)
        }
    }
}
