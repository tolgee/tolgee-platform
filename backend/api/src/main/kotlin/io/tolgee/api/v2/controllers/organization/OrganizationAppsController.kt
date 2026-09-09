package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.activity.RequestActivity
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.request.RegisterAppRequest
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.hateoas.organization.apps.AppManifestPreviewModel
import io.tolgee.hateoas.organization.apps.AppManifestPreviewModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresOrganizationScopes
import io.tolgee.service.apps.AppInstallService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/apps"])
@Tag(name = "Organization Apps")
class OrganizationAppsController(
  private val organizationHolder: OrganizationHolder,
  private val appInstallService: AppInstallService,
  private val appInstallModelAssembler: AppInstallModelAssembler,
  private val appManifestPreviewModelAssembler: AppManifestPreviewModelAssembler,
) {
  @PostMapping("/preview")
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @Operation(
    summary = "Preview a Tolgee app manifest",
    description =
      "Fetches the manifest at the given URL and returns its parsed contents (including the requested " +
        "scopes and a `manifestHash`) without persisting anything. Used to show a consent prompt before " +
        "registering or installing; pass the hash back so the write can reject a manifest that changed.",
  )
  fun preview(
    @PathVariable organizationId: Long,
    @RequestBody @Valid data: RegisterAppRequest,
  ): AppManifestPreviewModel {
    val fetched = appInstallService.previewManifest(data.manifestUrl)
    return appManifestPreviewModelAssembler.toModel(fetched)
  }

  @PostMapping
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @RequestActivity(ActivityType.APP_INSTALL)
  @Operation(
    summary = "Install a Tolgee app",
    description =
      "Fetches the manifest at the given URL and installs the app it describes for the " +
        "organization. The app must already be registered on this server and available to the " +
        "organization: when it is not registered, the call fails with the `app_not_registered` code, " +
        "and the caller may register it - becoming its owner - through the owned-apps endpoint. No " +
        "credentials are disclosed here.",
  )
  fun install(
    @PathVariable organizationId: Long,
    @RequestBody @Valid data: RegisterAppRequest,
  ): AppInstallModel {
    val install =
      appInstallService.install(
        organization = organizationHolder.organizationEntity,
        manifestUrl = data.manifestUrl,
        manifestHash = data.manifestHash,
      )
    return appInstallModelAssembler.toModel(install)
  }

  @GetMapping
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @Operation(
    summary = "List installed apps",
    description = "Returns every app this organization has installed.",
  )
  fun list(
    @PathVariable organizationId: Long,
  ): CollectionModel<AppInstallModel> {
    return appInstallModelAssembler.toCollectionModel(appInstallService.findAll(organizationId))
  }

  @PostMapping("/{installId}/refresh")
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @RequiresSuperAuthentication
  @RequestActivity(ActivityType.APP_UPDATE)
  @Operation(
    summary = "Refresh an installed app's manifest and approve its current scopes",
    description =
      "Re-fetches the app's manifest and updates the stored snapshot. As the organization's consent " +
        "authority, this also approves the scopes the manifest now requests: the install adopts that " +
        "scope set, clearing any pending permission requests. This is the only path that may widen an " +
        "install's granted scopes.",
  )
  fun refresh(
    @PathVariable organizationId: Long,
    @PathVariable installId: Long,
  ): AppInstallModel {
    return appInstallModelAssembler.toModel(appInstallService.refresh(organizationId, installId))
  }

  @DeleteMapping("/{installId}")
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @RequestActivity(ActivityType.APP_UNINSTALL)
  @Operation(
    summary = "Remove app",
    description = "Uninstalls the app from the organization.",
  )
  fun remove(
    @PathVariable organizationId: Long,
    @PathVariable installId: Long,
  ) {
    appInstallService.remove(organizationId, installId)
  }
}
