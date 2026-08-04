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
      "Server-wide secret that lets an app register itself without a signed-in user, by calling " +
        "`POST /v2/public/apps/self-register` with the `X-Tolgee-App-Registration-Secret` header. " +
        "Lets apps be connected to a running server without restarting it. Unset (the default) " +
        "disables self-registration entirely.\n" +
        "\n" +
        ":::danger\n" +
        "Anyone holding this secret can register an app into any organization and receive its " +
        "credentials. Treat it like an admin credential: keep it long and random, and leave it " +
        "unset unless you need self-registration.\n" +
        ":::\n\n",
  )
  var registrationSecret: String? = null

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
}
