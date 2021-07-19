/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.invitation.OrganizationInvitationModel
import io.tolgee.api.v2.hateoas.invitation.OrganizationInvitationModelAssembler
import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.organization.OrganizationModelAssembler
import io.tolgee.api.v2.hateoas.organization.UserAccountWithOrganizationRoleModel
import io.tolgee.api.v2.hateoas.organization.UserAccountWithOrganizationRoleModelAssembler
import io.tolgee.api.v2.hateoas.project.ProjectModel
import io.tolgee.api.v2.hateoas.project.ProjectModelAssembler
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.OrganizationDto
import io.tolgee.dtos.request.OrganizationInviteUserDto
import io.tolgee.dtos.request.OrganizationRequestParamsDto
import io.tolgee.dtos.request.SetOrganizationRoleDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.views.OrganizationView
import io.tolgee.model.views.ProjectView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.*
import org.springdoc.api.annotations.ParameterObject
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
@Tag(name = "Organizations")
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class OrganizationController(
  private val organizationService: OrganizationService,
  private val pagedProjectResourcesAssembler: PagedResourcesAssembler<ProjectView>,
  private val arrayResourcesAssembler: PagedResourcesAssembler<OrganizationView>,
  private val arrayUserResourcesAssembler: PagedResourcesAssembler<UserAccountWithOrganizationRoleView>,
  private val organizationModelAssembler: OrganizationModelAssembler,
  private val userAccountWithOrganizationRoleModelAssembler: UserAccountWithOrganizationRoleModelAssembler,
  private val tolgeeProperties: TolgeeProperties,
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val projectService: ProjectService,
  private val projectModelAssembler: ProjectModelAssembler,
  private val invitationService: InvitationService,
  private val organizationInvitationModelAssembler: OrganizationInvitationModelAssembler
) {

  @PostMapping
  @Transactional
  @Operation(summary = "Creates organization")
  fun create(@RequestBody @Valid dto: OrganizationDto): ResponseEntity<OrganizationModel> {
    if (!this.tolgeeProperties.authentication.userCanCreateOrganizations &&
      authenticationFacade.userAccount.role != UserAccount.Role.ADMIN
    ) {
      throw PermissionException()
    }
    this.organizationService.create(dto).let {
      return ResponseEntity(
        organizationModelAssembler.toModel(OrganizationView.of(it, OrganizationRoleType.OWNER)), HttpStatus.CREATED
      )
    }
  }

  @GetMapping("/{id:[0-9]+}")
  @Operation(summary = "Returns organization by ID")
  fun get(@PathVariable("id") id: Long): OrganizationModel? {
    organizationService.get(id)?.let {
      val roleType = organizationRoleService.getTypeOrThrow(id)
      return OrganizationView.of(it, roleType).toModel()
    }
      ?: throw NotFoundException()
  }

  @GetMapping("/{slug:.*[a-z].*}")
  @Operation(summary = "Returns organization by address part")
  fun get(@PathVariable("slug") slug: String): OrganizationModel {
    organizationService.get(slug)?.let {
      val roleType = organizationRoleService.getTypeOrThrow(it.id!!)
      return OrganizationView.of(it, roleType).toModel()
    }
      ?: throw NotFoundException()
  }

  @GetMapping("", produces = [MediaTypes.HAL_JSON_VALUE])
  @Operation(summary = "Returns all organizations, which is current user allowed to view")
  fun getAll(
    @ParameterObject pageable: Pageable,
    params: OrganizationRequestParamsDto
  ): PagedModel<OrganizationModel>? {
    val organizations = organizationService.findPermittedPaged(pageable, params)
    return arrayResourcesAssembler.toModel(organizations, organizationModelAssembler)
  }

  @PutMapping("/{id:[0-9]+}")
  @Operation(summary = "Updates organization data")
  fun update(@PathVariable("id") id: Long, @RequestBody @Valid dto: OrganizationDto): OrganizationModel {
    organizationRoleService.checkUserIsOwner(id)
    return this.organizationService.edit(id, editDto = dto).toModel()
  }

  @DeleteMapping("/{id:[0-9]+}")
  @Operation(summary = "Deletes organization and all its projects")
  fun delete(@PathVariable("id") id: Long) {
    organizationRoleService.checkUserIsOwner(id)
    organizationService.delete(id)
  }

  @GetMapping("/{id:[0-9]+}/users")
  @Operation(summary = "Returns all users in organization")
  fun getAllUsers(
    @PathVariable("id") id: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?
  ): PagedModel<UserAccountWithOrganizationRoleModel> {
    organizationRoleService.checkUserIsMemberOrOwner(id)
    val allInOrganization = userAccountService.getAllInOrganization(id, pageable, search)
    return arrayUserResourcesAssembler.toModel(allInOrganization, userAccountWithOrganizationRoleModelAssembler)
  }

  @PutMapping("/{id:[0-9]+}/leave")
  @Operation(summary = "Removes current user from organization")
  fun leaveOrganization(@PathVariable("id") id: Long) {
    organizationService.get(id)?.let {
      if (!organizationService.isThereAnotherOwner(id)) {
        throw ValidationException(Message.ORGANIZATION_HAS_NO_OTHER_OWNER)
      }
      organizationRoleService.checkUserIsMemberOrOwner(id)
      organizationRoleService.leave(id)
    } ?: throw NotFoundException()
  }

  @PutMapping("/{organizationId:[0-9]+}/users/{userId:[0-9]+}/set-role")
  @Operation(summary = "Sets user role (Owner or Member)")
  fun setUserRole(
    @PathVariable("organizationId") organizationId: Long,
    @PathVariable("userId") userId: Long,
    @RequestBody dto: SetOrganizationRoleDto
  ) {
    organizationRoleService.checkUserIsOwner(organizationId)
    organizationRoleService.setMemberRole(organizationId, userId, dto)
  }

  @DeleteMapping("/{organizationId:[0-9]+}/users/{userId:[0-9]+}")
  @Operation(summary = "Removes user from organization")
  fun removeUser(
    @PathVariable("organizationId") organizationId: Long,
    @PathVariable("userId") userId: Long
  ) {
    organizationRoleService.checkUserIsOwner(organizationId)
    organizationRoleService.removeUser(organizationId, userId)
  }

  @GetMapping("/{id:[0-9]+}/projects")
  @Operation(summary = "Returns all organization projects")
  fun getAllProjects(
    @PathVariable("id") id: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?
  ): PagedModel<ProjectModel> {
    return organizationService.get(id)?.let {
      organizationRoleService.checkUserIsMemberOrOwner(it.id!!)
      projectService.findAllInOrganization(it.id!!, pageable, search).let { projects ->
        pagedProjectResourcesAssembler.toModel(projects, projectModelAssembler)
      }
    } ?: throw NotFoundException()
  }

  @GetMapping("/{slug:.*[a-z].*}/projects")
  @Operation(summary = "Returns all organization projects")
  fun getAllProjects(
    @PathVariable("slug") slug: String,
    @ParameterObject pageable: Pageable,
    @RequestParam("search") search: String?
  ): PagedModel<ProjectModel> {
    return organizationService.get(slug)?.let {
      organizationRoleService.checkUserIsMemberOrOwner(it.id!!)
      projectService.findAllInOrganization(it.id!!, pageable, search).let { projects ->
        pagedProjectResourcesAssembler.toModel(projects, projectModelAssembler)
      }
    } ?: throw NotFoundException()
  }

  @PutMapping("/{id:[0-9]+}/invite")
  @Operation(summary = "Generates user invitation link for organization")
  fun inviteUser(
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
  fun getInvitations(@PathVariable("organizationId") id: Long):
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
