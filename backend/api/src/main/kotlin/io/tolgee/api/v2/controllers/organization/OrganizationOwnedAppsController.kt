package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.apps.AppDeliveryOutcomeModel
import io.tolgee.hateoas.apps.AppSecretModel
import io.tolgee.hateoas.apps.AppSecretModelAssembler
import io.tolgee.hateoas.apps.AppSecretRotationModel
import io.tolgee.dtos.request.apps.InstallAppIntoOrganizationRequest
import io.tolgee.dtos.request.apps.RollAppSecretRequest
import io.tolgee.hateoas.apps.AppWebhookSecretModel
import jakarta.validation.Valid
import io.tolgee.hateoas.organization.apps.AppInstallModel
import io.tolgee.hateoas.organization.apps.AppInstallModelAssembler
import io.tolgee.hateoas.organization.apps.InstallableOrganizationModel
import io.tolgee.hateoas.organization.apps.InstallingOrganizationModel
import io.tolgee.hateoas.organization.apps.OwnedAppModel
import io.tolgee.hateoas.organization.apps.OwnedAppModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.apps.AppAvailabilityService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.apps.AppOwnerRemovalService
import io.tolgee.service.apps.AppSecretRotationService
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppService
import io.tolgee.service.apps.AppWebhookSecretService
import io.tolgee.service.organization.OrganizationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * What an organization may do with an app it **registered**, as opposed to one it merely installed:
 * rotate the app-level credentials and take the app off every organization on the server.
 *
 * Every endpoint resolves the app within the organization, so an organization that installed
 * somebody else's app reaches none of this — app-level credentials are the owner's alone.
 */
@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/owned-apps"])
@Tag(name = "Organization Owned Apps")
class OrganizationOwnedAppsController(
  private val appService: AppService,
  private val appSecretService: AppSecretService,
  private val appSecretRotationService: AppSecretRotationService,
  private val appWebhookSecretService: AppWebhookSecretService,
  private val appAvailabilityService: AppAvailabilityService,
  private val appOwnerRemovalService: AppOwnerRemovalService,
  private val appInstallService: AppInstallService,
  private val organizationService: OrganizationService,
  private val authenticationFacade: AuthenticationFacade,
  private val ownedAppModelAssembler: OwnedAppModelAssembler,
  private val appSecretModelAssembler: AppSecretModelAssembler,
  private val appInstallModelAssembler: AppInstallModelAssembler,
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

  @GetMapping("/{appId}/secrets")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
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
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Roll the app's client secret",
    description =
      "Mints a replacement secret and puts every other active one on a deadline. The new secret is " +
        "both returned here — the only place it is disclosed — and pushed to the app over the " +
        "lifecycle channel; `secret.delivery` says whether the app received it. The old secrets " +
        "keep working until `previousExpiresAt` — the `graceSeconds` window — and can be revoked " +
        "earlier by hand; there is no immediate cutover, because a received delivery does not prove " +
        "the app adopted the secret. Refused while the app already has " +
        "the maximum number of active secrets. Installs, availability and per-project enablements " +
        "are untouched.",
  )
  fun rollSecret(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @RequestBody(required = false) @Valid body: RollAppSecretRequest?,
  ): AppSecretRotationModel {
    val app = appService.getOwned(organizationId, appId)
    val request = body ?: RollAppSecretRequest()
    val rotation = appSecretRotationService.rotate(app, request.graceSeconds)
    return AppSecretRotationModel(
      secret =
        appSecretModelAssembler.toModelWithSecret(
          rotation.issued.secret,
          rotation.issued.plaintextSecret,
          rotation.delivery,
        ),
      previousExpiresAt = rotation.previousExpiresAt?.time,
    )
  }

  @DeleteMapping("/{appId}/secrets/{secretId}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Revoke an app-level client secret",
    description =
      "Ends a secret at once — how a rotation's grace window is cut short, and the kill switch for a " +
        "leaked credential. Revoking the app's only active secret is refused unless `force=true`, so " +
        "an ordinary revoke cannot cut the app off by mistake; `force` also invalidates every access " +
        "token already minted from the app's secrets. Idempotent.",
  )
  fun revokeSecret(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
    @PathVariable secretId: Long,
    @RequestParam(required = false, defaultValue = "false") force: Boolean,
  ): AppSecretModel {
    val app = appService.getOwned(organizationId, appId)
    return appSecretModelAssembler.toModel(appSecretService.revoke(app.id, secretId, force = force))
  }

  @GetMapping("/{appId}/webhook-secret")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Reveal the app's current webhook signing secret",
    description =
      "Unlike a client secret, the webhook signing secret is stored in the clear — Tolgee needs it " +
        "to sign every delivery — so the owner can read it back here to configure the app by hand.",
  )
  fun getWebhookSecret(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ): AppWebhookSecretModel {
    val app = appService.getOwned(organizationId, appId)
    return AppWebhookSecretModel(secret = appService.resolveWebhookSecret(app.id))
  }

  @PostMapping("/{appId}/webhook-secret")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Rotate the app's webhook signing secret",
    description =
      "Mints a new webhook signing secret. The new secret is both returned here — the only place it " +
        "is disclosed — and delivered to the app, signed with the old secret so a running app adopts " +
        "it automatically; the `delivery` field says whether it landed. A running app keeps accepting " +
        "the old secret during the overlap and drops it on its own next rotation.",
  )
  fun rotateWebhookSecret(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ): AppWebhookSecretModel {
    val app = appService.getOwned(organizationId, appId)
    val rotation = appWebhookSecretService.rotate(app.id)
    return AppWebhookSecretModel(
      secret = rotation.newSecret,
      delivery =
        rotation.delivery?.let {
          AppDeliveryOutcomeModel(attempted = it.attempted, delivered = it.delivered, error = it.error)
        },
    )
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
  ): CollectionModel<InstallingOrganizationModel> {
    requireServerAdmin()
    val app = appService.getOwned(organizationId, appId)
    return CollectionModel.of(
      appInstallService.findInstallingOrganizations(app.id).map {
        InstallingOrganizationModel(it.id, it.name, it.slug)
      },
    )
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
        author = authenticationFacade.authenticatedUserEntity,
      )
    return appInstallModelAssembler.toModel(result)
  }

  @DeleteMapping("/{appId}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Remove the app from every organization",
    description =
      "Deregisters the app and uninstalls it from every organization that installed it, revoking " +
        "its credentials. Only the owner may do this; an organization that installed the app " +
        "removes only its own install through `DELETE /apps/{installId}`.",
  )
  fun removeEverywhere(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ) {
    val app = appService.getOwned(organizationId, appId)
    appOwnerRemovalService.removeEverywhere(app.id)
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
