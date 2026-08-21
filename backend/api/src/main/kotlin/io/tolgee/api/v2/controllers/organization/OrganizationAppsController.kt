package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.RegisterAppRequest
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.hateoas.organization.apps.AppManifestPreviewModel
import io.tolgee.hateoas.organization.apps.AppManifestPreviewModelAssembler
import io.tolgee.hateoas.organization.apps.AvailableAppModel
import io.tolgee.hateoas.organization.apps.AvailableAppModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppService
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
  private val authenticationFacade: AuthenticationFacade,
  private val appInstallService: AppInstallService,
  private val appService: AppService,
  private val appInstallModelAssembler: AppInstallModelAssembler,
  private val appManifestPreviewModelAssembler: AppManifestPreviewModelAssembler,
  private val availableAppModelAssembler: AvailableAppModelAssembler,
) {
  @PostMapping("/preview")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Preview a Tolgee app manifest",
    description =
      "Fetches the manifest at the given URL and returns its parsed contents (including the requested scopes) " +
        "without persisting anything. Used by the registration UI to show a consent prompt before installing.",
  )
  fun preview(
    @PathVariable organizationId: Long,
    @RequestBody @Valid data: RegisterAppRequest,
  ): AppManifestPreviewModel {
    val fetched = appInstallService.previewManifest(data.manifestUrl)
    return appManifestPreviewModelAssembler.toModel(fetched)
  }

  @PostMapping
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Install a Tolgee app",
    description =
      "Fetches the manifest at the given URL and installs the app it describes for the " +
        "organization. The app must already be registered on this server: when it is not, the call " +
        "fails with the `app_not_registered` code, and the caller may register it — becoming its " +
        "owner — through `POST /register`. No credentials are disclosed here: the app reaches its " +
        "new install with its app-level credentials.",
  )
  fun install(
    @PathVariable organizationId: Long,
    @RequestBody @Valid data: RegisterAppRequest,
  ): AppInstallModel {
    val result =
      appInstallService.install(
        organization = organizationHolder.organizationEntity,
        manifestUrl = data.manifestUrl,
      )
    return appInstallModelAssembler.toModel(result)
  }

  @PostMapping("/register")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Register a Tolgee app and install it",
    description =
      "Registers the app described by the manifest and installs it for the organization, in one " +
        "operation. The organization becomes the app's owner, and the response is the only place " +
        "the app-level credentials are ever disclosed. When the app is already registered — by " +
        "another organization or by this one — it is only installed, and no app-level credentials " +
        "are returned.",
  )
  fun register(
    @PathVariable organizationId: Long,
    @RequestBody @Valid data: RegisterAppRequest,
  ): AppInstallModel {
    val result =
      appInstallService.register(
        organization = organizationHolder.organizationEntity,
        manifestUrl = data.manifestUrl,
      )
    return appInstallModelAssembler.toModel(result)
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

  @GetMapping("/available")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "List apps available on this server",
    description =
      "Apps a server admin has offered to every organization that this organization can still " +
        "install — it neither owns nor has already installed them. Installing one goes through the " +
        "same consent flow as any other app.",
  )
  fun listAvailable(
    @PathVariable organizationId: Long,
  ): CollectionModel<AvailableAppModel> {
    return availableAppModelAssembler.toCollectionModel(appService.listAvailableToInstall(organizationId))
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
