package io.tolgee.service.apps

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.apps.App
import org.springframework.stereotype.Service

/**
 * Authenticates a request that carries the app-level client credentials in its body — the token
 * endpoint, app-secret rotation, and install discovery. Kept in one place so every such endpoint
 * verifies the same way and stamps the secret's last use.
 */
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

    // First use synchronously (the revoke guard reads it), later uses async off the hot path. Split
    // here rather than inside the service so the @Async proxy is not bypassed by self-invocation.
    if (secret.lastUsedAt == null) {
      appSecretService.recordFirstUse(secret.id)
    } else {
      appSecretService.updateLastUsedAsync(secret.id, secret.lastUsedAt)
    }
    return app
  }
}
