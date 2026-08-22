package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.RegisterAppRequest
import io.tolgee.hateoas.apps.AppRegisteredModel
import io.tolgee.hateoas.apps.AppRegisteredModelAssembler
import io.tolgee.hateoas.organization.apps.OwnedAppModel
import io.tolgee.hateoas.organization.apps.OwnedAppModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppService
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * What an organization does with the apps it **registered**, as opposed to ones it merely installed:
 * publish a new app (register) and read its apps. Every endpoint resolves the app within the
 * organization, so an organization that installed somebody else's app reaches none of this.
 */
@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/owned-apps"])
@Tag(name = "Organization Owned Apps")
class OrganizationOwnedAppsController(
  private val organizationHolder: OrganizationHolder,
  private val appService: AppService,
  private val appInstallService: AppInstallService,
  private val ownedAppModelAssembler: OwnedAppModelAssembler,
  private val appRegisteredModelAssembler: AppRegisteredModelAssembler,
) {
  @PostMapping
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Register a Tolgee app",
    description =
      "Registers the app described by the manifest and, unless `install` is false, installs it for " +
        "the organization. The organization becomes the app's owner, and the response is the only " +
        "place the app-level credentials are ever disclosed. When the app is already registered, it " +
        "is only installed and no app-level credentials are returned.",
  )
  fun register(
    @PathVariable organizationId: Long,
    @RequestBody @Valid data: RegisterAppRequest,
  ): AppRegisteredModel {
    val result =
      appInstallService.register(
        organization = organizationHolder.organizationEntity,
        manifestUrl = data.manifestUrl,
        manifestHash = data.manifestHash,
        install = data.install,
      )
    return appRegisteredModelAssembler.toModel(result)
  }

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
}
