package io.tolgee.api.v2.controllers.organization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.apps.AppManifest
import io.tolgee.dtos.request.RegisterAppRequest
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.hateoas.organization.apps.AppManifestPreviewModel
import io.tolgee.hateoas.organization.apps.AppRegistrationResponseModel
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.apps.AppInstallService
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/apps"])
@Tag(name = "Organization Apps")
class OrganizationAppsController(
  private val organizationHolder: OrganizationHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val appInstallService: AppInstallService,
  private val appInstallModelAssembler: AppInstallModelAssembler,
  private val objectMapper: ObjectMapper,
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
    @RequestBody data: RegisterAppRequest,
  ): AppManifestPreviewModel {
    val fetched = appInstallService.previewManifest(data.manifestUrl)
    return AppManifestPreviewModel(
      appId = fetched.manifest.id,
      name = fetched.manifest.name,
      version = fetched.manifest.version,
      baseUrl = fetched.manifest.baseUrl,
      modules = fetched.manifest.modules,
      requestedScopes = fetched.scopes.map { it.value },
      requestedWebhookEvents = fetched.webhookEvents.toList(),
    )
  }

  @PostMapping
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Register a Tolgee app",
    description = "Fetches the manifest at the given URL and registers the app for the organization.",
  )
  fun register(
    @PathVariable organizationId: Long,
    @RequestBody data: RegisterAppRequest,
  ): AppRegistrationResponseModel {
    val result =
      appInstallService.register(
        organization = organizationHolder.organizationEntity,
        manifestUrl = data.manifestUrl,
        author = authenticationFacade.authenticatedUserEntity,
      )
    val install = result.install
    val manifest = objectMapper.readValue<AppManifest>(install.manifestJson)
    return AppRegistrationResponseModel(
      id = install.id,
      manifestUrl = install.manifestUrl,
      appId = install.appId,
      name = install.name,
      version = install.version,
      baseUrl = install.baseUrl,
      modules = manifest.modules,
      scopes = install.grantedScopes.map { it.value },
      webhookEvents = install.webhookSubscriptions.toList(),
      webhookUrl = install.webhookUrl,
      clientId = install.clientId,
      clientSecretPrefix = install.clientSecretPrefix,
      webhookSecret = install.webhookSecret,
      decoratorsUrl = manifest.decoratorsUrl,
      clientSecret = result.plaintextClientSecret,
    )
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

  @PatchMapping("/{installId}/manifest-url")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Update manifest URL",
    description =
      "Repoints an existing install at a new manifest URL and re-fetches the manifest from there. " +
        "The new manifest must declare the same `id` as the original. Useful for development: a tunnel " +
        "URL that changes on every restart can be swapped in without re-installing the app.",
  )
  fun updateManifestUrl(
    @PathVariable organizationId: Long,
    @PathVariable installId: Long,
    @RequestBody body: RegisterAppRequest,
  ): AppInstallModel {
    val install =
      appInstallService.updateManifestUrl(
        organizationId = organizationId,
        installId = installId,
        manifestUrl = body.manifestUrl,
        allowScopeWidening = true,
      )
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
