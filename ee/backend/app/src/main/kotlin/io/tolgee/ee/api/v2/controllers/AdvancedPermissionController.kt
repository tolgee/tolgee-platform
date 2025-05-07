package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.project.SetPermissionLanguageParams
import io.tolgee.ee.service.EePermissionService
import io.tolgee.facade.ProjectPermissionFacade
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.openApiDocs.OpenApiEeExtension
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.RequiresProjectPermissions
import io.tolgee.service.organization.OrganizationRoleService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v2/")
@Tag(name = "Advanced permissions")
@OpenApiEeExtension
class AdvancedPermissionController(
  private val projectPermissionFacade: ProjectPermissionFacade,
  private val eePermissionService: EePermissionService,
  private val projectHolder: ProjectHolder,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val organizationRoleService: OrganizationRoleService,
) {
  @Suppress("MVCPathVariableInspection")
  @PutMapping("projects/{projectId:[0-9]+}/users/{userId}/set-permissions")
  @Operation(
    summary = "Set user's project permission",
    description = "Set user's granular (scope-based) direct project permission",
  )
  @RequiresProjectPermissions([ Scope.MEMBERS_EDIT ])
  @RequiresSuperAuthentication
  fun setUsersPermissions(
    @PathVariable("userId") userId: Long,
    @Parameter(
      description = "Granted scopes",
      example = """["translations.view", "translations.edit"]""",
    )
    @RequestParam
    scopes: List<String>?,
    @ParameterObject params: SetPermissionLanguageParams,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(
      projectHolder.project.organizationOwnerId!!,
      Feature.GRANULAR_PERMISSIONS,
    )
    val parsedScopes = Scope.parse(scopes)
    projectPermissionFacade.checkNotCurrentUser(userId)
    eePermissionService.setUserDirectPermission(
      projectId = projectHolder.project.id,
      userId = userId,
      languages = projectPermissionFacade.getLanguages(params, projectHolder.project.id),
      scopes = parsedScopes,
    )
  }

  @PutMapping("organizations/{organizationId:[0-9]+}/set-base-permissions")
  @Operation(
    summary = "Set organization base permission",
    description =
      "Set default granular (scope-based) permissions for organization users, " +
        "who don't have direct project permissions set.",
  )
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun setBasePermissions(
    @PathVariable organizationId: Long,
    @Parameter(
      description = "Granted scopes to all projects for all organization users without direct project permissions set.",
      example = """["translations.view", "translations.edit"]""",
    )
    @RequestParam
    scopes: List<String>,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(organizationId, Feature.GRANULAR_PERMISSIONS)
    val parsedScopes = Scope.parse(scopes)
    eePermissionService.setOrganizationBasePermission(organizationId, parsedScopes)
  }
}
