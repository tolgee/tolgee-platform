package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(
  description = "Configuration of session tracking and the authentication audit log.",
  displayName = "Session audit",
)
class SessionAuditProperties {
  @DocProperty(
    description =
      "How long a session's revocation state is cached in memory before it is re-read from the " +
        "database. This bounds how long a session revoked on one instance can stay usable on the " +
        "others when Redis is not configured; with Redis, revocations are broadcast immediately.",
    defaultExplanation = "1 minute",
  )
  var sessionCacheTtlMs: Long = 60000

  @DocProperty(
    description = "Maximum number of sessions kept in the in-memory revocation cache per instance.",
  )
  var sessionCacheMaxSize: Long = 100000

  @DocProperty(
    description =
      "Minimum interval between database writes of a session's last-used timestamp. Higher values " +
        "reduce write load on the authentication path at the cost of a less precise timestamp.",
    defaultExplanation = "5 minutes",
  )
  var sessionLastUsedUpdateIntervalMs: Long = 300000

  @DocProperty(
    description =
      "How long authentication audit events (logins, failed logins, token operations) are kept " +
        "before being purged. They contain IP addresses, so tune this to your compliance " +
        "requirements.",
    defaultExplanation = "2 years",
  )
  var auditEventRetentionDays: Long = 730

  @DocProperty(
    description = "How long expired sessions are kept before being purged.",
    defaultExplanation = "30 days",
  )
  var expiredSessionRetentionDays: Long = 30

  @DocProperty(
    description = "Enable scheduled purging of old authentication audit events and expired sessions.",
  )
  var purgeEnabled: Boolean = true

  @DocProperty(
    description =
      "How many reverse proxies sit in front of Tolgee. Addresses recorded as evidence - the " +
        "session list and the authentication audit log - are read this many entries from the end " +
        "of `X-Forwarded-For`, because a proxy appends the address it actually saw while a client " +
        "can only prepend entries of its own. Leave at 0 when nothing proxies Tolgee, which uses " +
        "the connection's address instead; set it to the number of trusted hops otherwise. Getting " +
        "this too high lets clients forge their own address.",
    defaultExplanation = "0, no proxy",
  )
  var trustedProxyCount: Int = 0

  @DocProperty(
    description =
      "Absolute path to a MaxMind-format GeoIP city database (`.mmdb`), used to show an approximate " +
        "location next to each session. No database is bundled with Tolgee; without this the session " +
        "list shows the IP address only. Both MaxMind GeoLite2 and DB-IP Lite databases work.",
    defaultExplanation = "not set, locations disabled",
  )
  var geoIpDatabasePath: String? = null

  @DocProperty(
    description = "Delay between purge runs in milliseconds.",
    defaultExplanation = "6 hours",
  )
  var purgeDelayMs: Long = 21600000
}
