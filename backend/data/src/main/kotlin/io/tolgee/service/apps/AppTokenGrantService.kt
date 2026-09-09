package io.tolgee.service.apps

import io.tolgee.Metrics
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.security.authentication.AppTokenService
import org.springframework.stereotype.Service

@Service
class AppTokenGrantService(
  private val appCredentialAuthenticator: AppCredentialAuthenticator,
  private val appInstallService: AppInstallService,
  private val appTokenService: AppTokenService,
  private val metrics: Metrics,
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

    if (installId == null) {
      val token = appTokenService.mintAppLevelToken(app.id)
      metrics.recordAppTokenMinted()
      return token
    }

    val install =
      appInstallService.findOwnInstall(app.id, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_NOT_FOUND)
    val token = appTokenService.mintInstallContextToken(install.id)
    metrics.recordAppTokenMinted()
    return token
  }

  companion object {
    const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
  }
}
