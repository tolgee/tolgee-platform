package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.apps.AppSecretModel
import io.tolgee.hateoas.apps.AppSecretModelAssembler
import io.tolgee.hateoas.organization.apps.OwnedAppModel
import io.tolgee.hateoas.organization.apps.OwnedAppModelAssembler
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.service.apps.AppOwnerRemovalService
import io.tolgee.service.apps.AppSecretRotationService
import io.tolgee.service.apps.AppSecretService
import io.tolgee.service.apps.AppService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
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
  private val appOwnerRemovalService: AppOwnerRemovalService,
  private val ownedAppModelAssembler: OwnedAppModelAssembler,
  private val appSecretModelAssembler: AppSecretModelAssembler,
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
      "Returns every secret of the app, revoked ones included, without disclosing any of them. " +
        "They are the app's only long-lived credentials — everything the app does across every " +
        "organization that installed it starts from them. `lastUsedAt` is what tells you whether " +
        "the app has moved to a newly issued secret and the old one can be revoked.",
  )
  fun listSecrets(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ): CollectionModel<AppSecretModel> {
    val app = appService.getOwned(organizationId, appId)
    return appSecretModelAssembler.toCollectionModel(appSecretService.list(app.id))
  }

  @PostMapping("/{appId}/secrets")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Issue an additional app-level client secret",
    description =
      "Phase one of an app-level rotation: mints a second secret while every existing one keeps " +
        "working. The app's installs, their organization availability and their per-project " +
        "enablements are all untouched. The new secret is both returned here — the only place it " +
        "is ever disclosed — and pushed to the app over the lifecycle channel; the `delivery` " +
        "field says whether the app took it.",
  )
  fun issueSecret(
    @PathVariable organizationId: Long,
    @PathVariable appId: Long,
  ): AppSecretModel {
    val app = appService.getOwned(organizationId, appId)
    val rotation = appSecretRotationService.issueAndDeliver(app)
    return appSecretModelAssembler.toModelWithSecret(
      rotation.issued.secret,
      rotation.issued.plaintextSecret,
      rotation.delivery,
    )
  }

  @DeleteMapping("/{appId}/secrets/{secretId}")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @Operation(
    summary = "Revoke an app-level client secret",
    description =
      "Phase two of a rotation: the secret stops authenticating immediately and every other one is " +
        "untouched. Refused while the app has not demonstrably moved to a replacement (no other " +
        "live secret has been used yet), so an ordinary rotation cannot cut the app off by mistake. " +
        "Pass `force=true` to override — the kill switch for a leaked secret, where cutting the app " +
        "off now is the point. Idempotent.",
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
}
