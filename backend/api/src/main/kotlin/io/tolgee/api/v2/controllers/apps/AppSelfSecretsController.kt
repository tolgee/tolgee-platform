package io.tolgee.api.v2.controllers.apps

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.apps.AppInstallSecretModel
import io.tolgee.hateoas.apps.AppInstallSecretModelAssembler
import io.tolgee.security.authentication.AllowAppOwnInstallAccess
import io.tolgee.security.authentication.AppAuthentication
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.apps.AppInstallSecretService
import io.tolgee.service.apps.AppInstallService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.hateoas.CollectionModel
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Lets an app rotate its **own** client secret unattended, so an operator never has to copy one by
 * hand and an app publisher can script a rotation across every server it is installed on. There is
 * no publisher identity to authenticate as — each install has its own credentials — so a leak is
 * recovered from per install, which is exactly what this endpoint is for.
 *
 * The install is taken from the token's own claims and never from the request; the secret id in the
 * path is only ever resolved within that install. See [AllowAppOwnInstallAccess].
 */
@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/apps/self"])
@Tag(name = "App Self Service")
class AppSelfSecretsController(
  private val authenticationFacade: AuthenticationFacade,
  private val appInstallService: AppInstallService,
  private val appInstallSecretService: AppInstallSecretService,
  private val appInstallSecretModelAssembler: AppInstallSecretModelAssembler,
) {
  @GetMapping("/secrets")
  @AllowAppOwnInstallAccess
  @Operation(
    summary = "List the calling app's own client secrets",
    description =
      "Returns every secret of the calling install, revoked ones included, without disclosing any " +
        "of them. Requires a token from the client-credentials grant.",
  )
  fun list(): CollectionModel<AppInstallSecretModel> {
    val install = requireInstallContext().appInstall
    return appInstallSecretModelAssembler.toCollectionModel(appInstallSecretService.list(install.id))
  }

  @PostMapping("/secrets")
  @AllowAppOwnInstallAccess
  @Operation(
    summary = "Issue an additional client secret for the calling app",
    description =
      "Mints a fresh secret for the calling install and returns it — the only place it is ever " +
        "disclosed. The secret the call authenticated with keeps working, so the app can store the " +
        "new one and only then revoke the old one.",
  )
  fun issue(): AppInstallSecretModel {
    val install = requireInstallContext().appInstall
    val issued = appInstallService.issueSecret(install)
    return appInstallSecretModelAssembler.toModelWithSecret(issued.secret, issued.plaintextSecret)
  }

  @DeleteMapping("/secrets/{secretId}")
  @AllowAppOwnInstallAccess
  @Operation(
    summary = "Revoke one of the calling app's own client secrets",
    description =
      "The secret stops authenticating immediately. Revoking the install's last live secret is " +
        "refused here — an app authenticates with a secret, so it would lock itself out of this " +
        "very endpoint. Issue the replacement first. Idempotent.",
  )
  fun revoke(
    @PathVariable secretId: Long,
  ): AppInstallSecretModel {
    val install = requireInstallContext().appInstall
    val revoked = appInstallSecretService.revoke(install.id, secretId, allowRevokingLast = false)
    return appInstallSecretModelAssembler.toModel(revoked)
  }

  private fun requireInstallContext(): AppAuthentication {
    if (!authenticationFacade.isAppAuth) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    val appAuthentication = authenticationFacade.appAuthentication
    if (!appAuthentication.isInstallContext) throw PermissionException(Message.APP_ACCESS_FORBIDDEN)
    return appAuthentication
  }
}
