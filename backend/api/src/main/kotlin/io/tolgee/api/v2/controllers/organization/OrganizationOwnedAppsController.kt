package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.request.apps.InstallAppIntoOrganizationRequest
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.hateoas.organization.apps.InstallableOrganizationModel
import io.tolgee.hateoas.organization.apps.InstallingOrganizationModel
import io.tolgee.hateoas.organization.apps.InstallingOrganizationModelAssembler
import io.tolgee.hateoas.organization.apps.OwnedAppModel
import io.tolgee.hateoas.organization.apps.OwnedAppModelAssembler
import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppService
import io.tolgee.service.organization.OrganizationService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * What an organization may do with an app it **registered**, as opposed to one it merely installed:
 * offer it server-wide and enrol other organizations into it.
 *
 * Every endpoint resolves the app within the organization, so an organization that installed
 * somebody else's app reaches none of this.
 */
@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/owned-apps"])
@Tag(name = "Organization Owned Apps")
class OrganizationOwnedAppsController(
  private val appService: AppService,
  private val appAvailabilityService: AppAvailabilityService,
  private val appInstallService: AppInstallService,
  private val organizationService: OrganizationService,
  private val authenticationFacade: AuthenticationFacade,
  private val ownedAppModelAssembler: OwnedAppModelAssembler,
  private val appInstallModelAssembler: AppInstallModelAssembler,
  private val installingOrganizationModelAssembler: InstallingOrganizationModelAssembler,
  private val pagedInstallingOrganizationsAssembler: PagedResourcesAssembler<Organization>,
) {
  @GetMapping
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "List the apps this organization owns",
    description =
      "Returns every app the organization registered, together with the health of its manifest. " +
        "An app the organization also installed appears in the installed apps list as well.",
  )
  fun list(
    @PathVariable organizationId: Long,
  ): CollectionModel<OwnedAppModel> {
    return ownedAppModelAssembler.toCollectionModel(appService.listOwned(organizationId))
  }

  @GetMapping("/{appId}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(summary = "Get an owned app")
  fun get(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ): OwnedAppModel {
    return ownedAppModelAssembler.toModel(appService.getOwned(organizationId, appId))
  }

  @PutMapping("/{appId}/availability")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Offer the app to every organization, or withdraw it (server admin only)",
    description =
      "Server-admin action, shown on the owner's Apps page under the server-admin controls: makes " +
        "the app installable by every organization, or (with `available=false`) withdraws it to the " +
        "owner and disables it in projects of organizations that could only reach it through the " +
        "offer. An organization owner who is not a server admin may not call this.",
  )
  fun setAvailability(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @RequestParam available: Boolean,
  ): OwnedAppModel {
    requireServerAdmin()
    val app = appService.getOwned(organizationId, appId)
    appAvailabilityService.setAvailableToAllOrganizations(app.id, available)
    return ownedAppModelAssembler.toModel(appService.getOwned(organizationId, appId))
  }

  @GetMapping("/{appId}/installations")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "List the organizations that have the app installed (server admin only)",
    description =
      "Server-admin action backing the installations view: which organizations currently hold the " +
        "app. To uninstall it from one, a server admin opens that organization's Apps page and " +
        "removes it there. An organization owner who is not a server admin may not call this.",
  )
  fun installations(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @ParameterObject pageable: Pageable,
    @RequestParam("search", required = false) search: String?,
  ): PagedModel<InstallingOrganizationModel> {
    requireServerAdmin()
    val app = appService.getOwned(organizationId, appId)
    val page = appInstallService.findInstallingOrganizations(app.id, search, pageable)
    return pagedInstallingOrganizationsAssembler.toModel(page, installingOrganizationModelAssembler)
  }

  @GetMapping("/{appId}/installable-organizations")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Search organizations to install the app into (server admin only)",
    description =
      "Server-admin action backing the one-step install picker: the top organizations matching " +
        "`search`, by name. An organization owner who is not a server admin may not call this.",
  )
  fun installableOrganizations(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @RequestParam(required = false) search: String?,
  ): CollectionModel<InstallableOrganizationModel> {
    requireServerAdmin()
    appService.getOwned(organizationId, appId)
    val page =
      organizationService.findAllPaged(
        PageRequest.of(0, 10, Sort.by("name")),
        search,
        authenticationFacade.authenticatedUser.id,
      )
    return CollectionModel.of(page.content.map { InstallableOrganizationModel(it.id, it.name, it.slug) })
  }

  @PostMapping("/{appId}/install-into")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Install the app into an organization (server admin only)",
    description =
      "Server-admin action, shown on the owner's Apps page under the server-admin controls: installs " +
        "the app into the chosen organization directly, bypassing the availability gate — the " +
        "one-step first-party enrolment. Idempotent: an organization that already has the app keeps " +
        "its one install. An organization owner who is not a server admin may not call this.",
  )
  fun installInto(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @RequestBody body: InstallAppIntoOrganizationRequest,
  ): AppInstallModel {
    requireServerAdmin()
    val app = appService.getOwned(organizationId, appId)
    val target = organizationService.get(body.organizationId)
    val result =
      appInstallService.installForOrganizationByAdmin(
        app = app,
        targetOrganization = target,
      )
    return appInstallModelAssembler.toModel(result)
  }

  /**
   * The org-owner gate already lets a server admin in (they bypass org roles), but it also lets an
   * ordinary owner in — and offering an app server-wide is the admin's decision, not the owner's, so
   * this narrows it back to admins.
   */
  private fun requireServerAdmin() {
    if (!authenticationFacade.authenticatedUser.isAdmin()) throw PermissionException()
  }
}
