package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.oauth2")
@DocProperty(
  description = "Settings for Tolgee acting as an OAuth 2.1 authorization server (browser-extension login, MCP).",
  displayName = "OAuth2 authorization server",
)
class OAuth2ServerProperties {
  @DocProperty(
    description =
      "Exact redirect URIs of the Tolgee browser extension, e.g. `https://<extension-id>.chromiumapp.org/`. " +
        "The extension OAuth client is only registered when this is set.",
    defaultValue = "",
  )
  var browserExtensionRedirectUris: List<String> = listOf()

  @DocProperty(
    description =
      "Loopback redirect URIs of the Tolgee CLI (RFC 8252), e.g. `http://127.0.0.1:9876/callback`. Prefer the " +
        "loopback IP literal over `localhost`, which RFC 8252 section 7.3 marks NOT RECOMMENDED: a client " +
        "resolving `localhost` may end up listening on interfaces other than the loopback one. The CLI " +
        "OAuth client is only registered when this is set.\n" +
        "\n" +
        ":::info\n" +
        "A loopback redirect cannot be tied to one local application, so any process on the machine that knows " +
        "the client id can start an authorization for it. The user still has to approve the consent screen, " +
        "but leave this unset unless the CLI is actually in use.\n" +
        ":::\n\n",
    defaultValue = "",
  )
  var cliRedirectUris: List<String> = listOf()

  @DocProperty(description = "How long an issued OAuth access token stays valid, in minutes.")
  var accessTokenValidityMinutes: Long = 30

  @DocProperty(
    description =
      "How long an issued OAuth refresh token stays valid, in days. Each refresh issues a new one and restarts " +
        "this window, so it bounds how long a grant may sit unused — not how long it may live.",
  )
  var refreshTokenValidityDays: Long = 30

  @DocProperty(
    description = "How long an authorization code can be exchanged for tokens after it was issued, in seconds.",
  )
  var authorizationCodeValiditySeconds: Long = 300

  @DocProperty(
    description =
      "How long the user has to complete the consent screen before the pending authorization goes stale, in seconds.",
  )
  var consentValiditySeconds: Long = 900

  @DocProperty(
    description =
      "How long a spent OAuth grant is kept after its last credential expired, in days. It holds a used " +
        "code's row so a replayed code is still recognised. A consent the user never completed is not kept for " +
        "this window — it is deleted once its own short deadline passes.",
  )
  var grantRetentionDays: Long = 7

  @DocProperty(
    description =
      "Cron expression for the job that removes spent grants past their retention window and consents the " +
        "user never completed. Spring's six-field format (second, minute, hour, day, month, weekday).",
  )
  var grantCleanupCron: String = DEFAULT_GRANT_CLEANUP_CRON

  companion object {
    const val DEFAULT_GRANT_CLEANUP_CRON = "0 0 3 * * *"
  }
}
