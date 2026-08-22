package io.tolgee.api.v2.controllers.administration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.hateoas.organization.SimpleOrganizationModel
import io.tolgee.hateoas.organization.SimpleOrganizationModelAssembler
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
import org.springframework.web.bind.annotation.DeleteMapping
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
  private val simpleOrganizationModelAssembler: SimpleOrganizationModelAssembler,
  private val pagedOrganizationsAssembler: PagedResourcesAssembler<Organization>,
) : IController {
  @PutMapping("/{appId}/available-to-all")
  @Operation(summary = "Make the app available to all organizations")
  @RequiresSuperAuthentication
  fun setAvailableToAll(
    @PathVariable appId: Long,
  ) {
    appService.getRegistered(appId)
    appAvailabilityService.setAvailableToAll(appId)
  }

  @DeleteMapping("/{appId}/available-to-all")
  @Operation(
    summary = "Stop making the app available to all organizations",
    description =
      "Disables the app in every non-owner project that could only reach it through availability to all organizations.",
  )
  @RequiresSuperAuthentication
  fun clearAvailableToAll(
    @PathVariable appId: Long,
  ) {
    appService.getRegistered(appId)
    appAvailabilityService.clearAvailableToAll(appId)
  }

  @GetMapping("/{appId}/available-organizations")
  @Operation(
    summary = "List the organizations the app is available to",
    description = "The organizations granted the app besides the owner, searchable and paged.",
  )
  @RequiresSuperAuthentication
  fun availableOrganizations(
    @PathVariable appId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
  ): PagedModel<SimpleOrganizationModel> {
    appService.getRegistered(appId)
    val page = appAvailabilityService.findAvailableOrganizations(appId, search, pageable)
    return pagedOrganizationsAssembler.toModel(page, simpleOrganizationModelAssembler)
  }

  @PutMapping("/{appId}/available-organizations/{organizationId}")
  @Operation(summary = "Make the app available to one organization")
  @RequiresSuperAuthentication
  fun addAvailableOrganization(
    @PathVariable appId: Long,
    @PathVariable organizationId: Long,
  ) {
    appService.getRegistered(appId)
    organizationService.get(organizationId)
    appAvailabilityService.addAvailableOrganization(appId, organizationId)
  }

  @DeleteMapping("/{appId}/available-organizations/{organizationId}")
  @Operation(
    summary = "Stop making the app available to one organization",
    description =
      "Disables the app in that organization's projects unless it stays reachable through availability to all organizations.",
  )
  @RequiresSuperAuthentication
  fun removeAvailableOrganization(
    @PathVariable appId: Long,
    @PathVariable organizationId: Long,
  ) {
    appService.getRegistered(appId)
    appAvailabilityService.removeAvailableOrganization(appId, organizationId)
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
  ): PagedModel<SimpleOrganizationModel> {
    appService.getRegistered(appId)
    val page = appInstallService.findInstallingOrganizations(appId, search, pageable)
    return pagedOrganizationsAssembler.toModel(page, simpleOrganizationModelAssembler)
  }
}
