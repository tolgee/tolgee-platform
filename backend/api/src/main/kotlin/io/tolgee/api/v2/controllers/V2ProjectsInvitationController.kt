/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.EeInvitationService
import io.tolgee.constants.Message
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.dtos.request.project.ProjectInviteUserDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.facade.ProjectPermissionFacade
import io.tolgee.hateoas.invitation.ProjectInvitationModel
import io.tolgee.hateoas.invitation.ProjectInvitationModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.InvitationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress(names = ["MVCPathVariableInspection", "SpringJavaInjectionPointsAutowiringInspection"])
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/projects"])
@Tag(name = "Projects")
class V2ProjectsInvitationController(
  private val projectHolder: ProjectHolder,
  private val invitationService: InvitationService,
  private val projectInvitationModelAssembler: ProjectInvitationModelAssembler,
  private val projectPermissionFacade: ProjectPermissionFacade,
  private val eeInvitationService: EeInvitationService,
) {
  @PutMapping("/{projectId}/invite")
  @Operation(summary = "Generates user invitation link for project")
  @RequiresProjectPermissions([ Scope.MEMBERS_EDIT ])
  @RequiresSuperAuthentication
  fun inviteUser(
    @RequestBody @Valid
    invitation: ProjectInviteUserDto,
  ): ProjectInvitationModel {
    validatePermissions(invitation)

    val languagesPermissions = projectPermissionFacade.getLanguages(invitation, projectHolder.project.id)

    val params =
      CreateProjectInvitationParams(
        project = projectHolder.projectEntity,
        type = invitation.type,
        scopes = invitation.scopes,
        email = invitation.email,
        name = invitation.name,
        languagePermissions = languagesPermissions,
      )

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
}
