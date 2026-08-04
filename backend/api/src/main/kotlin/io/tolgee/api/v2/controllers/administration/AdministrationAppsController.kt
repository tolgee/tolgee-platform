package io.tolgee.api.v2.controllers.administration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.hateoas.organization.apps.AppAvailableOrganizationModel
import io.tolgee.hateoas.organization.apps.AppAvailableOrganizationModelAssembler
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.model.apps.AppInstall
import io.tolgee.openApiDocs.OpenApiSelfHostedExtension
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppInstallService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/administration/apps"])
@Tag(
  name = "Server Administration",
  description =
    "**Only for self-hosted instances** \n\n" +
      "Manages native (server-level) Tolgee Apps and the organizations they are available to.",
)
@OpenApiSelfHostedExtension
class AdministrationAppsController(
  private val appInstallService: AppInstallService,
  private val appAvailabilityService: AppAvailabilityService,
  private val appInstallModelAssembler: AppInstallModelAssembler,
  private val appAvailableOrganizationModelAssembler: AppAvailableOrganizationModelAssembler,
  private val pagedAppInstallResourcesAssembler: PagedResourcesAssembler<AppInstall>,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  @GetMapping
  @Operation(
    summary = "List native apps",
    description =
      "Returns the apps registered at server level — those belonging to no organization. " +
        "The client secret is never disclosed here.",
  )
  @RequiresSuperAuthentication
  fun list(
    @ParameterObject
    @SortDefault(sort = ["name"])
    pageable: Pageable,
  ): PagedModel<AppInstallModel> {
    val installs = appInstallService.findAllNativePaged(pageable)
    return pagedAppInstallResourcesAssembler.toModel(installs, appInstallModelAssembler)
  }

  @DeleteMapping("/{installId}")
  @Operation(
    summary = "Deregister a native app",
    description =
      "Removes the native app from the server: its availability for every organization, its " +
        "enablement in every project and the install itself. Its client credentials stop working.",
  )
  @RequiresSuperAuthentication
  fun deregister(
    @PathVariable installId: Long,
  ) {
    appInstallService.remove(organizationId = null, installId = installId)
  }

  @GetMapping("/{installId}/organizations")
  @Operation(
    summary = "List organizations the app is available to",
    description = "Returns the organizations allowed to enable this native app in their projects.",
  )
  @RequiresSuperAuthentication
  fun listOrganizations(
    @PathVariable installId: Long,
  ): CollectionModel<AppAvailableOrganizationModel> {
    val install = appInstallService.getNative(installId)
    val organizations = appAvailabilityService.listOrganizations(install.id)
    return appAvailableOrganizationModelAssembler.toCollectionModel(organizations)
  }

  @PutMapping("/{installId}/organizations/all")
  @Operation(
    summary = "Make the app available to all organizations",
    description =
      "Allows every organization — including organizations created later — to enable this native " +
        "app. Explicit per-organization grants are kept, so revoking this falls back to them. " +
        "Idempotent.",
  )
  @RequiresSuperAuthentication
  fun grantToAllOrganizations(
    @PathVariable installId: Long,
  ) {
    appAvailabilityService.grantToAllOrganizations(installId)
  }

  @DeleteMapping("/{installId}/organizations/all")
  @Operation(
    summary = "Revoke the app's availability for all organizations",
    description =
      "Drops the blanket availability and disables the app in every project whose organization has " +
        "no explicit grant. Idempotent.",
  )
  @RequiresSuperAuthentication
  fun revokeFromAllOrganizations(
    @PathVariable installId: Long,
  ) {
    appAvailabilityService.revokeFromAllOrganizations(installId)
  }

  @PutMapping("/{installId}/organizations/{organizationId}")
  @Operation(
    summary = "Make the app available to an organization",
    description = "Allows the organization's projects to enable this native app. Idempotent.",
  )
  @RequiresSuperAuthentication
  fun grant(
    @PathVariable installId: Long,
    @PathVariable organizationId: Long,
  ) {
    appAvailabilityService.grant(
      installId = installId,
      organizationId = organizationId,
      author = authenticationFacade.authenticatedUserEntity,
    )
  }

  @DeleteMapping("/{installId}/organizations/{organizationId}")
  @Operation(
    summary = "Revoke the app's availability for an organization",
    description =
      "Revokes the explicit grant and disables the app in every project of that organization — " +
        "unless the app is available to all organizations, which keeps covering it. " +
        "Idempotent — no-op when it was not available.",
  )
  @RequiresSuperAuthentication
  fun revoke(
    @PathVariable installId: Long,
    @PathVariable organizationId: Long,
  ) {
    appAvailabilityService.revoke(installId = installId, organizationId = organizationId)
  }
}
