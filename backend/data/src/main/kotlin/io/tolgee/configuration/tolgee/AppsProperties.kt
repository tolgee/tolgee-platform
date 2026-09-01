package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.apps")
@DocProperty(description = "Configuration for Tolgee Apps (plugins).", displayName = "Apps")
class AppsProperties {
  @DocProperty(
    description =
      "Enables the Tolgee Apps feature. When disabled, all apps endpoints are absent " +
        "and the web UI hides every apps-related screen.",
  )
  var enabled: Boolean = false

  @DocProperty(
    description =
      "SHA-256 hash, base64-encoded, of the app self-registration secret. When set, an app " +
        "presenting the matching plaintext may register itself into any organization on this " +
        "server without a signed-in user — meant for first-party apps deployed alongside the " +
        "server, whose deployment injects the plaintext. Unset (the default), self-registration " +
        "is disabled and apps are registered by hand in the UI.\n" +
        "\n" +
        "Only the hash lives in this configuration, so a leaked config file does not hand out a " +
        "usable credential. Generate it from your chosen plaintext with:\n" +
        "\n" +
        "```\n" +
        "printf '%s' \"<secret>\" | openssl dgst -sha256 -binary | base64\n" +
        "```",
  )
  var registrationSecretHash: String? = null

  @DocProperty(
    description =
      "When enabled, Tolgee App manifest URLs may target otherwise-blocked address ranges — " +
        "loopback, private/site-local, link-local, IPv6 unique-local, multicast and " +
        "wildcard/any-local addresses. Useful for local development and integration testing.\n" +
        "\n" +
        ":::danger\n" +
        "This removes SSRF protection for app manifest fetches. Keep it **disabled** on production " +
        "and multi-tenant servers — anyone able to register an app could otherwise reach internal " +
        "services.\n" +
        ":::\n\n",
  )
  var allowLocalAddresses: Boolean = false

  @DocProperty(
    description =
      "Lifetime of app access tokens in milliseconds — both the token the dashboard iframe uses and " +
        "the one an app backend gets from the client-credentials grant.\n" +
        "\n" +
        "Deliberately much shorter than `tolgee.authentication.jwt-expiration`: an app token is " +
        "handed to third-party code, so it should be cheap to leak. Scope changes and app " +
        "disablement take effect immediately regardless of this value — it only bounds how long a " +
        "*stolen* token stays usable.",
    defaultExplanation = "= 1 hour",
  )
  var tokenExpiration: Long = 60 * 60 * 1000

  @DocProperty(
    description =
      "How often every registered app's manifest is re-fetched to check the app is still there.",
  )
  var manifestHealthCheckPeriodMinutes: Long = 60

  @DocProperty(
    description =
      "How long a manifest URL must keep failing before the app is marked unhealthy and its owner " +
        "notified. Both this and `manifest-unhealthy-min-failures` must be exceeded, so a single " +
        "failure — or a burst of them inside a short window — never marks an app unhealthy.",
    defaultExplanation = "= 1 day",
  )
  var manifestUnhealthyAfterHours: Long = 24

  @DocProperty(
    description =
      "How many consecutive failed manifest checks are needed before an app may be marked unhealthy.",
  )
  var manifestUnhealthyMinFailures: Int = 3

  @DocProperty(
    description =
      "How long an app stays unhealthy before it is removed from every organization that " +
        "installed it. Counted from the moment it was marked unhealthy, so the total grace an app " +
        "gets is this plus `manifest-unhealthy-after-hours`.",
    defaultExplanation = "= 14 days",
  )
  var manifestReapAfterUnhealthyDays: Long = 14

  @DocProperty(
    description =
      "Whether an app whose manifest stayed unreachable past the grace period is removed from " +
        "every organization that installed it. Disabled by default: health state and owner " +
        "notifications are always recorded, but the destructive step is opt-in, because a long " +
        "egress or DNS outage on Tolgee's side is indistinguishable from an app that is gone.\n" +
        "\n" +
        "An app whose manifest is reachable but no longer valid is never removed — somebody is " +
        "still serving it, so its author is there to fix it.",
  )
  var reapUnreachableApps: Boolean = false
}
