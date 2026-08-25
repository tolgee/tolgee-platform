package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppSecret
import org.springframework.stereotype.Service

/** Verifies app-level client credentials carried in a request body, and stamps the secret's use. */
@Service
class AppCredentialAuthenticator(
  private val appService: AppService,
  private val appSecretService: AppSecretService,
) {
  fun authenticate(
    clientId: String,
    clientSecret: String,
  ): App {
    val app =
      appService.resolveByClientId(clientId)
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    val secret =
      appSecretService.findLiveMatching(app.id, clientSecret)
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)

    stampUse(secret)
    return app
  }

  // Split out of the service so its @Async proxy on updateLastUsedAsync survives self-invocation.
  private fun stampUse(secret: AppSecret) {
    if (secret.lastUsedAt == null) {
      appSecretService.recordFirstUse(secret.id)
      return
    }
    appSecretService.updateLastUsedAsync(secret.id, secret.lastUsedAt)
  }
}
