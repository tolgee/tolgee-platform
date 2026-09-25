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

  @DocProperty(
    description =
      "How long, in minutes, a recorded client withdrawal stays reversible. Taking the metadata document down " +
        "ends every grant of that client; if the document answers again within this window the mark is lifted, " +
        "which covers a mis-deploy. After it, the retirement is permanent for those grants, so a publisher who " +
        "republishes a fixed build at the same `client_id` does not hand back a grant they retired on purpose. " +
        "Users of that client simply consent again. A mark is also liftable while no check has happened since it " +
        "was made, whatever this window says: how soon a client is read again is a queue position, not a " +
        "duration, so on a busy instance the window alone would close before the publisher's turn came round.",
  )
  var cimdWithdrawalGraceMinutes: Long = 60

  @DocProperty(
    description =
      "How long, in days, a grant of a client that identifies itself with a metadata document may keep working " +
        "while that document cannot be read. A short outage at the publisher must not sign every user out, so an " +
        "unreadable document is tolerated - but not forever, because then anyone able to keep this server from " +
        "reading it could keep a retired client alive. It counts only time this server spent *trying*: a grant " +
        "whose document nothing has attempted to read is never refused, so a job that stops running cannot sign " +
        "out every third-party user. Lower it on an instance that wants third-party grants checked more strictly.",
  )
  var cimdVerificationMaxAgeDays: Long = 7

  @DocProperty(
    description =
      "Cron expression for the job that re-reads the metadata documents of clients holding grants. It is what " +
        "notices that a publisher retired a client, and what keeps that client's grants inside " +
        "`cimd-verification-max-age-days`. One instance runs each round.",
    defaultValue = DEFAULT_CIMD_CHECK_CRON,
  )
  var cimdCheckCron: String = DEFAULT_CIMD_CHECK_CRON

  @DocProperty(
    description =
      "How many client documents one round of that job reads. The round takes the ones checked longest ago, so " +
        "this and the cron together set how long a full pass takes - which must stay well inside " +
        "`cimd-verification-max-age-days`.",
  )
  var cimdCheckBatchSize: Int = 100

  @DocProperty(
    description =
      "How long, in minutes, before the same client's document is read again. It is set by how quickly a " +
        "publisher taking their document down should take effect, not by `cimd-verification-max-age-days`, which " +
        "would be satisfied by reading a document once every few days. The cost of a short interval is outbound " +
        "traffic to third-party hosts; the cost of a long one is a retired client staying usable for longer.",
  )
  var cimdCheckIntervalMinutes: Long = 15

  @DocProperty(
    description =
      "How many different clients identifying themselves with a metadata document one account may hold " +
        "authorizations for. Each one joins the work list of the job that re-reads those documents, and that job " +
        "has a fixed rate, so without a cap a single scripted account decides how quickly a publisher's " +
        "retirement is noticed for everyone else. Raise it only if real users legitimately connect more " +
        "third-party apps than this.",
  )
  var cimdMaxClientsPerUser: Long = 25

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
    const val DEFAULT_CIMD_CHECK_CRON = "0 */5 * * * *"
  }
}
