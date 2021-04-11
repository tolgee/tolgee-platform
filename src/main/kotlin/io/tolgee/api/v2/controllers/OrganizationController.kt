/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.tolgee.api.v2.hateoas.invitation.OrganizationInvitationModel
import io.tolgee.api.v2.hateoas.invitation.OrganizationInvitationModelAssembler
import io.tolgee.api.v2.hateoas.organization.*
import io.tolgee.api.v2.hateoas.repository.RepositoryModel
import io.tolgee.api.v2.hateoas.repository.RepositoryModelAssembler
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.OrganizationDto
import io.tolgee.dtos.request.OrganizationInviteUserDto
import io.tolgee.dtos.request.OrganizationRequestParamsDto
import io.tolgee.dtos.request.SetOrganizationRoleDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.*
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.views.OrganizationView
import io.tolgee.model.views.RepositoryView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.*
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations", "/api/organizations"])
open class OrganizationController(
        private val organizationService: OrganizationService,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val pagedResourcesAssembler: PagedResourcesAssembler<Organization>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val pagedRepositoryResourcesAssembler: PagedResourcesAssembler<RepositoryView>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val arrayResourcesAssembler: PagedResourcesAssembler<OrganizationView>,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection")
        private val arrayUserResourcesAssembler: PagedResourcesAssembler<UserAccountWithOrganizationRoleView>,
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
    @Transactional
    open fun create(@RequestBody @Valid dto: OrganizationDto): ResponseEntity<OrganizationModel> {
        if (!this.tolgeeProperties.authentication.userCanCreateOrganizations
                && authenticationFacade.userAccount.role != UserAccount.Role.ADMIN) {
            throw PermissionException()
        }
        this.organizationService.create(dto).let {
            return ResponseEntity(organizationModelAssembler.toModel(OrganizationView.of(it, OrganizationRoleType.OWNER)), HttpStatus.CREATED)
        }
    }

    @GetMapping("/{id:[0-9]+}")
    open fun get(@PathVariable("id") id: Long): OrganizationModel? {
        organizationService.get(id)?.let {
            val roleType = organizationRoleService.getTypeOrThrow(id)
            return OrganizationView.of(it, roleType).toModel()
        }
                ?: throw NotFoundException()
    }

    @GetMapping("/{addressPart:.*[a-z].*}")
    open fun get(@PathVariable("addressPart") addressPart: String): OrganizationModel {
        organizationService.get(addressPart)?.let {
            val roleType = organizationRoleService.getTypeOrThrow(it.id!!)
            return OrganizationView.of(it, roleType).toModel()
        }
                ?: throw NotFoundException()
    }

    @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
    open fun getAll(pageable: Pageable, params: OrganizationRequestParamsDto): PagedModel<OrganizationModel>? {
        val organizations = organizationService.findPermittedPaged(pageable, params)
        return arrayResourcesAssembler.toModel(organizations, organizationModelAssembler)
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
        return arrayUserResourcesAssembler.toModel(allInOrganization, userAccountWithOrganizationRoleModelAssembler)
    }

    @PutMapping("/{id:[0-9]+}/leave")
    open fun leaveOrganization(@PathVariable("id") id: Long) {
        organizationService.get(id)?.let {
            if (!organizationService.isThereAnotherOwner(id)) {
                throw ValidationException(Message.ORGANIZATION_HAS_NO_OTHER_OWNER)
            }
            organizationRoleService.checkUserIsMemberOrOwner(id)
            organizationRoleService.leave(id)

        } ?: throw NotFoundException()
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
            invitationService.create(organization, invitation.roleType).let {
                organizationInvitationModelAssembler.toModel(it)
            }
        } ?: throw NotFoundException()
    }

    @GetMapping("/{organizationId}/invitations")
    @Operation(summary = "Returns all invitations to organization")
    open fun getInvitations(@PathVariable("organizationId") id: Long):
            CollectionModel<OrganizationInvitationModel> {
        val organization = organizationService.get(id) ?: throw NotFoundException()
        organizationRoleService.checkUserIsOwner(id)
        return invitationService.getForOrganization(organization).let {
            organizationInvitationModelAssembler.toCollectionModel(it)
        }
    }

    private fun OrganizationView.toModel(): OrganizationModel {
        return this@OrganizationController.organizationModelAssembler.toModel(this)
    }
}
