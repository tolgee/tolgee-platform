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
      "Whether `tolgee login` can sign users in against this instance. The CLI is registered wherever the " +
        "authorization server is live, so nothing has to be configured for browser login to work.\n" +
        "\n" +
        ":::info\n" +
        "A loopback redirect cannot be tied to one local application, so any process on the machine can start an " +
        "authorization as the CLI. The user still has to approve the consent screen, and the token can never " +
        "exceed what they are allowed to do, but an instance that will never see the CLI can turn it off here.\n" +
        ":::\n\n",
    defaultValue = "true",
    defaultExplanation =
      "The client is registered only where the issuer resolves: without `tolgee.back-end-url` (or " +
        "`tolgee.front-end-url`) the authorization server is off entirely and browser login cannot work.",
  )
  var cliEnabled: Boolean = true

  @DocProperty(
    description =
      "Loopback redirect URIs of the Tolgee CLI (RFC 8252), accepted instead of the `http://127.0.0.1/callback` " +
        "the CLI uses by default. The port is ignored either way, since a CLI takes whatever port the OS gives " +
        "it, so this is only needed for a build that listens elsewhere. Prefer the loopback IP literal over " +
        "`localhost`, which RFC 8252 section 8.3 marks NOT RECOMMENDED: a client resolving `localhost` may end " +
        "up listening on interfaces other than the loopback one.",
    defaultValue = "http://127.0.0.1/callback",
  )
  var cliRedirectUris: List<String> = listOf()

  @DocProperty(
    description =
      "Whether an unknown client may identify itself with a Client ID Metadata Document (an HTTPS URL as " +
        "`client_id`). This is what lets an MCP client an operator never registered ask for access, and it is the " +
        "one path on which an unauthenticated caller makes this server fetch a URL of their choosing — bounded by " +
        "the address checks, the fetch budget and the rate limit on the authorization endpoint. Turn it off on an " +
        "instance that should only ever serve clients it registered itself.",
    defaultValue = "true",
  )
  var cimdEnabled: Boolean = true

  @DocProperty(
    description =
      "Hosts allowed to present a Client ID Metadata Document (an HTTPS URL as `client_id`) so an unknown MCP " +
        "client can register itself. Empty (the default) allows any public host; set it to restrict CIMD to a " +
        "specific list, e.g. `claude.ai`. Loopback and private hosts are always refused regardless.",
    defaultValue = "",
  )
  var cimdAllowedHosts: List<String> = listOf()

  @DocProperty(description = "How long an issued OAuth access token stays valid, in minutes.")
  var accessTokenValidityMinutes: Long = 30

  @DocProperty(
    description =
      "How long an issued OAuth refresh token stays valid, in days. Each refresh issues a new one and restarts " +
        "this window, so it bounds how long a grant may sit unused — not how long it may live.",
  )
  var refreshTokenValidityDays: Long = 30

  @DocProperty(
    description =
      "Grace window, in seconds, during which replaying the refresh token that was just rotated away fails the " +
        "request without revoking the grant. It absorbs innocent collisions (two tabs, a lost response) instead of " +
        "signing the user out everywhere; a replay after the window, or of an older token, is still treated as theft.",
  )
  var refreshTokenGraceSeconds: Long = 60

  @DocProperty(
    description =
      "How many already-rotated refresh tokens are remembered per grant, so that replaying one is recognised as " +
        "theft and revokes the grant. This is the bound that binds for a *slow* client — a CLI used once a week " +
        "reaches this many weeks back. For a client rotating faster than this many times within " +
        "`refresh-token-history-min-days`, that window is what decides instead. Beyond both, a replay is still " +
        "refused, it just no longer revokes.",
  )
  var refreshTokenHistoryGenerations: Int = 50

  @DocProperty(
    description =
      "Minimum age, in days, before a rotated refresh token can be dropped from that history. This is the bound " +
        "that binds for a *fast* client, and the one to reach for if the table is growing. It also keeps the depth " +
        "above from being something a thief can force: rank depends only on how many rotations followed a row, and " +
        "a thief holding a stolen token can produce those in minutes — this makes eviction cost wall-clock time.",
  )
  var refreshTokenHistoryMinDays: Long = 7

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
