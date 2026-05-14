package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.RegisterAppRequest
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.apps.AppInstallService
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/apps"])
@Tag(name = "Organization Apps")
class OrganizationAppsController(
  private val organizationHolder: OrganizationHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val appInstallService: AppInstallService,
  private val appInstallModelAssembler: AppInstallModelAssembler,
) {
  @PostMapping
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Register a Tolgee app",
    description = "Fetches the manifest at the given URL and registers the app for the organization.",
  )
  fun register(
    @PathVariable organizationId: Long,
    @RequestBody data: RegisterAppRequest,
  ): AppInstallModel {
    val install =
      appInstallService.register(
        organization = organizationHolder.organizationEntity,
        manifestUrl = data.manifestUrl,
        author = authenticationFacade.authenticatedUserEntity,
      )
    return appInstallModelAssembler.toModel(install)
  }

  @GetMapping
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "List registered apps",
    description = "Returns all apps registered for the organization.",
  )
  fun list(
    @PathVariable organizationId: Long,
  ): CollectionModel<AppInstallModel> {
    val installs = appInstallService.findAll(organizationId)
    return appInstallModelAssembler.toCollectionModel(installs)
  }

  @PostMapping("/{installId}/refresh")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Refresh manifest",
    description = "Re-fetches the manifest from the registered URL and updates the stored snapshot.",
  )
  fun refresh(
    @PathVariable organizationId: Long,
    @PathVariable installId: Long,
  ): AppInstallModel {
    val install = appInstallService.refresh(organizationId, installId)
    return appInstallModelAssembler.toModel(install)
  }

  @DeleteMapping("/{installId}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Remove app",
    description = "Removes the registered app from the organization.",
  )
  fun remove(
    @PathVariable organizationId: Long,
    @PathVariable installId: Long,
  ) {
    appInstallService.remove(organizationId, installId)
  }
}
