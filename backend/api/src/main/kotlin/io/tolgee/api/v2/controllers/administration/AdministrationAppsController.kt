package io.tolgee.api.v2.controllers.administration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.hateoas.organization.apps.AppAvailabilityModel
import io.tolgee.hateoas.organization.apps.InstallingOrganizationModel
import io.tolgee.hateoas.organization.apps.InstallingOrganizationModelAssembler
import io.tolgee.model.Organization
import io.tolgee.openApiDocs.OpenApiSelfHostedExtension
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppService
import io.tolgee.service.organization.OrganizationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Server-admin management of a registered app: who it is available to, and who currently holds it.
 * Availability governs which organizations may self-install the app from its manifest - the admin
 * does not install it on their behalf.
 */
@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping("/v2/administration/apps")
@Tag(
  name = "Server Administration",
  description = "**Only for self-hosted instances** \n\n Manage which organizations a published app is available to.",
)
@OpenApiSelfHostedExtension
class AdministrationAppsController(
  private val appService: AppService,
  private val appAvailabilityService: AppAvailabilityService,
  private val appInstallService: AppInstallService,
  private val organizationService: OrganizationService,
  private val installingOrganizationModelAssembler: InstallingOrganizationModelAssembler,
  private val pagedInstallingOrganizationsAssembler: PagedResourcesAssembler<Organization>,
) : IController {
  @GetMapping("/{appId}/availability")
  @Operation(summary = "Get an app's availability set")
  @RequiresSuperAuthentication
  fun getAvailability(
    @PathVariable appId: Long,
  ): AppAvailabilityModel {
    appService.getRegistered(appId)
    return availabilityModel(appId)
  }

  @PutMapping("/{appId}/availability/all")
  @Operation(
    summary = "Offer the app to every organization, or withdraw the blanket offer",
    description =
      "With `available=true` the app becomes installable by every organization; with `available=false` " +
        "the blanket offer is withdrawn and the app is disabled in every non-owner project it could only " +
        "be reached through it.",
  )
  @RequiresSuperAuthentication
  fun setAvailableToAll(
    @PathVariable appId: Long,
    @RequestParam available: Boolean,
  ): AppAvailabilityModel {
    appService.getRegistered(appId)
    appAvailabilityService.setAvailableToAll(appId, available)
    return availabilityModel(appId)
  }

  @PutMapping("/{appId}/availability/organizations/{organizationId}")
  @Operation(
    summary = "Offer the app to one organization, or withdraw that grant",
    description =
      "With `available=false` the grant is withdrawn and the app is disabled in that organization's " +
        "projects unless it stays reachable through the blanket offer.",
  )
  @RequiresSuperAuthentication
  fun setAvailableToOrganization(
    @PathVariable appId: Long,
    @PathVariable organizationId: Long,
    @RequestParam available: Boolean,
  ): AppAvailabilityModel {
    appService.getRegistered(appId)
    organizationService.get(organizationId)
    appAvailabilityService.setAvailableToOrganization(appId, organizationId, available)
    return availabilityModel(appId)
  }

  @GetMapping("/{appId}/installations")
  @Operation(
    summary = "List the organizations that currently have the app installed",
    description = "Backs the installations view: which organizations hold the app, searchable and paged.",
  )
  @RequiresSuperAuthentication
  fun installations(
    @PathVariable appId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
  ): PagedModel<InstallingOrganizationModel> {
    appService.getRegistered(appId)
    val page = appInstallService.findInstallingOrganizations(appId, search, pageable)
    return pagedInstallingOrganizationsAssembler.toModel(page, installingOrganizationModelAssembler)
  }

  private fun availabilityModel(appId: Long): AppAvailabilityModel {
    val availability = appAvailabilityService.listAvailability(appId)
    return AppAvailabilityModel(
      availableToAll = availability.availableToAll,
      organizations = availability.organizations.map { installingOrganizationModelAssembler.toModel(it) },
    )
  }
}
