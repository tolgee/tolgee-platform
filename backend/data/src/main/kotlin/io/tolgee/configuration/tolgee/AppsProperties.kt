package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.apps")
@DocProperty(description = "Configuration for Tolgee Apps (plugins).", displayName = "Apps")
class AppsProperties {
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
}
