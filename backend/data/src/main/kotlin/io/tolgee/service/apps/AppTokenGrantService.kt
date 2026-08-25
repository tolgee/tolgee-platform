package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.security.authentication.AppTokenService
import org.springframework.stereotype.Service

/**
 * The client-credentials grant behind the app token endpoint: validates the grant, authenticates the
 * app credentials, and mints either an app-level token (no install) or an install-context token.
 */
@Service
class AppTokenGrantService(
  private val appCredentialAuthenticator: AppCredentialAuthenticator,
  private val appInstallService: AppInstallService,
  private val appTokenService: AppTokenService,
) {
  fun issueFromClientCredentials(
    grantType: String,
    clientId: String,
    clientSecret: String,
    installId: Long?,
  ): String {
    if (grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
      throw BadRequestException(Message.APP_UNSUPPORTED_GRANT_TYPE)
    }

    val app = appCredentialAuthenticator.authenticate(clientId, clientSecret)

    if (installId == null) return appTokenService.mintAppLevelToken(app.id)

    val install =
      appInstallService.findOwnInstall(app.id, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    return appTokenService.mintInstallContextToken(install.id)
  }

  companion object {
    const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
  }
}
