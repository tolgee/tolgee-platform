package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.request.RegisterAppRequest
import io.tolgee.dtos.request.apps.RotateAppSecretRequest
import io.tolgee.hateoas.apps.AppRegisteredModel
import io.tolgee.hateoas.apps.AppRegisteredModelAssembler
import io.tolgee.hateoas.apps.AppSecretModel
import io.tolgee.hateoas.apps.AppSecretModelAssembler
import io.tolgee.hateoas.apps.AppSecretRotationModel
import io.tolgee.hateoas.organization.apps.OwnedAppModel
import io.tolgee.hateoas.organization.apps.OwnedAppModelAssembler
import io.tolgee.model.enums.Scope
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.RequiresOrganizationScopes
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppOwnerRemovalService
import io.tolgee.service.apps.AppSecretService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
  private val appOwnerRemovalService: AppOwnerRemovalService,
  private val appSecretService: AppSecretService,
  private val appSecretModelAssembler: AppSecretModelAssembler,
) {
  @PostMapping
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @Operation(
    summary = "Register a Tolgee app",
    description =
      "Registers the app described by the manifest and, unless `install` is false, installs it for " +
        "the organization. The organization becomes the app's owner, and the response is the only " +
        "place the app-level credentials are ever disclosed. When the app is already registered the " +
        "call fails with `app_already_registered`; installing an existing app is the install endpoint.",
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
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
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
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @Operation(summary = "Get an owned app")
  fun get(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ): OwnedAppModel {
    return ownedAppModelAssembler.toModel(appService.getOwned(organizationId, appId))
  }

  @DeleteMapping("/{appId}")
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @Operation(
    summary = "Delete an owned app",
    description =
      "Takes the app down across the whole server: every organization's install is removed and the " +
        "app's credentials are revoked. Use this to retire an app or to kill a compromised one in a " +
        "single step, rather than uninstalling it per organization.",
  )
  fun delete(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ) {
    val app = appService.getOwned(organizationId, appId)
    appOwnerRemovalService.removeEverywhere(app.id)
  }

  @GetMapping("/{appId}/secrets")
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @Operation(
    summary = "List the app-level client secrets",
    description =
      "Returns every secret of the app, revoked and expired ones included, without disclosing any " +
        "of them. At rest an app has a single active secret; older ones, each with an `expiresAt`, " +
        "linger only while a rotation's grace window is open.",
  )
  fun listSecrets(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ): CollectionModel<AppSecretModel> {
    val app = appService.getOwned(organizationId, appId)
    return appSecretModelAssembler.toCollectionModel(appSecretService.list(app.id))
  }

  @PostMapping("/{appId}/secrets/rotate")
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @RequiresSuperAuthentication
  @Operation(
    summary = "Rotate the app's client secret",
    description =
      "Mints a replacement secret and puts every other active one on a deadline. The new secret is " +
        "returned here — the only place it is disclosed. The old secrets keep working until " +
        "`previousExpiresAt` — the `graceSeconds` window — and can be revoked earlier by hand; there " +
        "is no immediate cutover, because Tolgee cannot know the app has adopted the new secret. " +
        "Refused while the app already has the maximum number of active secrets. Installs, " +
        "availability and per-project enablements are untouched.",
  )
  fun rotateSecret(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @RequestBody(required = false) @Valid body: RotateAppSecretRequest?,
  ): AppSecretRotationModel {
    val app = appService.getOwned(organizationId, appId)
    val request = body ?: RotateAppSecretRequest()
    val rotation = appSecretService.rotate(app, request.graceSeconds)
    return AppSecretRotationModel(
      secret = appSecretModelAssembler.toModelWithSecret(rotation.issued.secret, rotation.issued.plaintextSecret),
      previousExpiresAt = rotation.previousExpiresAt?.time,
    )
  }

  @DeleteMapping("/{appId}/secrets/{secretId}")
  @RequiresOrganizationScopes([Scope.ORGANIZATION_APPS_MANAGE])
  @RequiresSuperAuthentication
  @Operation(
    summary = "Revoke an app-level client secret",
    description =
      "Ends this one secret at once — how a rotation's grace window is cut short, and how a leaked " +
        "credential is retired. Revoking the app's only active secret is refused unless `force=true`, " +
        "so an ordinary revoke cannot cut the app off by mistake; `force` also invalidates every " +
        "access token already minted from the app's secrets. It does not touch the app's OTHER live " +
        "secrets, so to fully contain a compromise revoke each secret (a holder of a live secret can " +
        "mint more) or delete the app, which retires every credential in one step. Idempotent.",
  )
  fun revokeSecret(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @PathVariable secretId: Long,
    @Parameter(
      description =
        "Revoke even when this is the app's last active secret, and invalidate every access token " +
          "already minted from the app's secrets. Use to retire a leaked credential; without it, " +
          "revoking the only active secret is refused so the app can't be cut off by mistake.",
    )
    @RequestParam(required = false, defaultValue = "false") force: Boolean,
  ): AppSecretModel {
    val app = appService.getOwned(organizationId, appId)
    return appSecretModelAssembler.toModel(appSecretService.revoke(app.id, secretId, force = force))
  }
}
