package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.project.SetPermissionLanguageParams
import io.tolgee.facade.ProjectPermissionFacade
import io.tolgee.model.enums.Scope
import io.tolgee.security.NeedsSuperJwtToken
import io.tolgee.security.project_auth.AccessWithProjectPermission
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.EePermissionService
import io.tolgee.service.organization.OrganizationRoleService
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/")
@Tag(name = "Advanced permissions")
class AdvancedPermissionController(
  private val projectPermissionFacade: ProjectPermissionFacade,
  private val eePermissionService: EePermissionService,
  private val projectHolder: ProjectHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val organizationRoleService: OrganizationRoleService
) {
  @Suppress("MVCPathVariableInspection")
  @PutMapping("projects/{projectId}/users/{userId}/set-permissions")
  @AccessWithProjectPermission(Scope.MEMBERS_EDIT)
  @Operation(summary = "Sets user's direct permission")
  @NeedsSuperJwtToken
  fun setUsersPermissions(
    @PathVariable("userId") userId: Long,
    @Schema(
      description = "Granted scopes",
      example = """["translations.view", "translations.edit"]"""
    )
    @RequestParam scopes: List<String>?,
    @ParameterObject params: SetPermissionLanguageParams
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId!!,
      Feature.GRANULAR_PERMISSIONS
    )
    val parsedScopes = Scope.parse(scopes)
    projectPermissionFacade.checkNotCurrentUser(userId)
    eePermissionService.setUserDirectPermission(
      projectId = projectHolder.project.id,
      userId = userId,
      languages = projectPermissionFacade.getLanguages(params, projectHolder.project.id),
      scopes = parsedScopes
    )
  }

  @PutMapping("organizations/{organizationId:[0-9]+}/set-base-permissions")
  @Operation(summary = "Sets organization base permission")
  fun setBasePermissions(
    @PathVariable organizationId: Long,
    @Schema(
      description = "Granted scopes to all projects for all organization users without direct project permissions set",
      example = """["translations.view", "translations.edit"]"""
    )
    @RequestParam scopes: List<String>
  ) {
    organizationRoleService.checkUserIsOwner(organizationId)
    enabledFeaturesProvider.checkFeatureEnabled(organizationId, Feature.GRANULAR_PERMISSIONS)
    val parsedScopes = Scope.parse(scopes)
    eePermissionService.setOrganizationBasePermission(organizationId, parsedScopes)
  }
}
