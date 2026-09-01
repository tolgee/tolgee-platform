package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.apps.AppTokenModel
import io.tolgee.security.authentication.AppTokenService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.DenyAppAccess
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.apps.AppInstallService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@ConditionalOnProperty(name = ["tolgee.apps.enabled"], havingValue = "true")
@RequestMapping(value = ["/v2/organizations/{organizationId:[0-9]+}/apps/{installId:[0-9]+}"])
@Tag(name = "Organization Apps")
@DenyAppAccess
class OrganizationAppTokenController(
  private val appInstallService: AppInstallService,
  private val appTokenService: AppTokenService,
  private val authenticationFacade: AuthenticationFacade,
) {
  @PostMapping("/token")
  @UseDefaultPermissions
  @ReadOnlyOperation
  @Operation(
    summary = "Mint a user-context app token",
    description =
      "Issues a short-lived JWT bound to (install, current user) that the dashboard iframe uses to " +
        "call Tolgee's REST API on behalf of the user. The token is organization-wide: it works on " +
        "every project the install is enabled for, always capped by the user's own permissions there.",
  )
  fun mintToken(
    @PathVariable organizationId: Long,
    @PathVariable installId: Long,
  ): AppTokenModel {
    appInstallService.find(organizationId, installId)
      ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    val token =
      appTokenService.mintUserContextToken(
        installId = installId,
        userId = authenticationFacade.authenticatedUser.id,
        isReadOnly = authenticationFacade.isReadOnly,
      )
    return AppTokenModel(token = token)
  }
}
