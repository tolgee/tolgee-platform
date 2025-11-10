package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.dtos.misc.CreateOrganizationInvitationParams
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.organization.OrganizationInviteUserDto
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.facade.ProjectPermissionFacade
import io.tolgee.hateoas.invitation.OrganizationInvitationModel
import io.tolgee.hateoas.invitation.OrganizationInvitationModelAssembler
import io.tolgee.hateoas.invitation.ProjectInvitationModel
import io.tolgee.hateoas.invitation.ProjectInvitationModelAssembler
import io.tolgee.hateoas.invitation.PublicInvitationModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authentication.WriteOperation
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.TranslationAgencyService
import io.tolgee.service.invitation.EeInvitationService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.SecurityService
import jakarta.validation.Valid
import org.springframework.hateoas.CollectionModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("")
@Tag(name = "Invitations", description = "These endpoints manage inviting new users to projects or organizations")
class V2InvitationController(
  private val securityService: SecurityService,
  private val organizationRoleService: OrganizationRoleService,
  private val projectService: ProjectService,
  private val projectHolder: ProjectHolder,
  private val invitationService: InvitationService,
  private val projectInvitationModelAssembler: ProjectInvitationModelAssembler,
  private val projectPermissionFacade: ProjectPermissionFacade,
  private val eeInvitationService: EeInvitationService,
  private val organizationService: OrganizationService,
  private val organizationInvitationModelAssembler: OrganizationInvitationModelAssembler,
  private val permissionService: PermissionService,
  private val authenticationFacade: AuthenticationFacade,
  private val translationAgencyService: TranslationAgencyService,
  private val publicInvitationModelAssembler: PublicInvitationModelAssembler,
) {
  @GetMapping("/v2/invitations/{code}/accept")
  @WriteOperation
  @Operation(
    summary =
      "Accepts invitation to project or organization " +
        "(deprecated: use PUT method instead)",
    deprecated = true,
  )
  fun acceptInvitation(
    @PathVariable("code") code: String?,
  ): ResponseEntity<Void> {
    invitationService.accept(code)
    return ResponseEntity(HttpStatus.OK)
  }

  @PutMapping("/v2/invitations/{code}/accept")
  @Operation(summary = "Accepts invitation to project or organization")
  fun acceptInvitationPut(
    @PathVariable("code") code: String?,
  ): ResponseEntity<Void> {
    invitationService.accept(code)
    return ResponseEntity(HttpStatus.OK)
  }

  @DeleteMapping("/v2/invitations/{invitationId}")
  @Operation(summary = "Deletes invitation by ID")
  fun deleteInvitation(
    @PathVariable("invitationId") id: Long,
  ): ResponseEntity<Void> {
    val invitation =
      invitationService.findById(id).orElseThrow {
        NotFoundException()
      }
    invitation.permission?.let {
      securityService.checkProjectPermission(
        invitation.permission!!.project!!.id,
        Scope.ADMIN,
      )
    }

    invitation.organizationRole?.let {
      organizationRoleService.checkUserCanDeleteInvitation(
        invitation.organizationRole!!.organization!!.id,
      )
    }

    invitationService.delete(invitation)
    return ResponseEntity(HttpStatus.OK)
  }

  @GetMapping("/v2/projects/{projectId:[0-9]+}/invitations")
  @Operation(summary = "Get project invitations")
  @RequiresProjectPermissions([Scope.MEMBERS_VIEW])
  @RequiresSuperAuthentication
  @AllowApiAccess
  fun getProjectInvitations(
    @PathVariable("projectId") id: Long,
  ): CollectionModel<ProjectInvitationModel> {
    val project = projectService.get(id)
    val invitations = invitationService.getForProject(project)
    return projectInvitationModelAssembler.toCollectionModel(invitations)
  }

  @PutMapping("/v2/projects/{projectId:[0-9]+}/invite")
  @Operation(summary = "Generate user invitation link for project")
  @RequiresProjectPermissions([Scope.MEMBERS_EDIT])
  @RequiresSuperAuthentication
  fun inviteUser(
    @RequestBody @Valid
    invitation: ProjectInviteUserDto,
  ): ProjectInvitationModel {
    validatePermissions(invitation)
    val currentUserPermissions =
      permissionService.findPermissionNonCached(
        projectHolder.project.id,
        authenticationFacade.authenticatedUser.id,
      )

    val languagesPermissions = projectPermissionFacade.getLanguages(invitation, projectHolder.project.id)

    val params =
      if (invitation.agencyId != null) {
        val agency = translationAgencyService.findById(invitation.agencyId!!)
        CreateProjectInvitationParams(
          project = projectHolder.projectEntity,
          type = invitation.type,
          scopes = invitation.scopes,
          email = agency.email,
          name = "Agency invitation",
          languagePermissions = languagesPermissions,
          agencyId = agency.id,
        )
      } else {
        CreateProjectInvitationParams(
          project = projectHolder.projectEntity,
          type = invitation.type,
          scopes = invitation.scopes,
          email = invitation.email,
          name = invitation.name,
          languagePermissions = languagesPermissions,
          agencyId = currentUserPermissions?.agency?.id,
        )
      }

    val created =
      if (!params.scopes.isNullOrEmpty()) {
        eeInvitationService.create(params)
      } else {
        invitationService.create(params)
      }

    return projectInvitationModelAssembler.toModel(created)
  }

  private fun validatePermissions(invitation: ProjectInviteUserDto) {
    if (!(invitation.scopes.isNullOrEmpty() xor (invitation.type == null))) {
      throw BadRequestException(Message.SET_EXACTLY_ONE_OF_SCOPES_OR_TYPE)
    }
  }

  @PutMapping("/v2/organizations/{id:[0-9]+}/invite")
  @Operation(
    summary = "Generate invitation link for organization",
    description =
      "Generates invitation link for organization, so users can join organization. " +
        "The invitation can also be sent to an e-mail address.",
  )
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresSuperAuthentication
  fun inviteUser(
    @RequestBody @Valid
    dto: OrganizationInviteUserDto,
    @PathVariable("id") id: Long,
  ): OrganizationInvitationModel {
    val organization = organizationService.get(id)

    val invitation =
      invitationService.create(
        CreateOrganizationInvitationParams(
          organization = organization,
          type = dto.roleType,
          email = dto.email,
          name = dto.name,
        ),
      )

    return organizationInvitationModelAssembler.toModel(invitation)
  }

  @GetMapping("/v2/organizations/{organizationId}/invitations")
  @Operation(summary = "Get all invitations to organization")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresSuperAuthentication
  fun getInvitations(
    @PathVariable("organizationId") id: Long,
  ): CollectionModel<OrganizationInvitationModel> {
    val organization = organizationService.find(id) ?: throw NotFoundException()
    return invitationService.getForOrganization(organization).let {
      organizationInvitationModelAssembler.toCollectionModel(it)
    }
  }
}
