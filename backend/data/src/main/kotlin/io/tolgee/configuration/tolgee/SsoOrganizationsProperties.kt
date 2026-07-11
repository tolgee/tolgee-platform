package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.sso-organizations")
@DocProperty(
  description =
    "Single sign-on (SSO) is an authentication process that allows a user to" +
      " access multiple applications with one set of login credentials. To use SSO" +
      " in Tolgee, can either configure global SSO settings in `sso-global` section or" +
      " in the per Organization mode by setting the `enable` to `true` in this section and configuring" +
      " it separately for each organization in the organization settings.\n\n" +
      "There is a significant difference between global and per organization SSO:" +
      " Global SSO can handle authentication for all server users no matter which organizations they belong to," +
      " while per organization SSO can handle authentication only for users of the organization and" +
      " such users cannot be members of any other organization. SSO users associated with per organization SSO have" +
      " no rights to create or manage organizations. Global SSO users should be invited to organizations they need to" +
      " have access to. Per organization SSO users are automatically added to the organization they belong to.",
  displayName = "Per-Organization Single Sign-On",
)
class SsoOrganizationsProperties {
  @E2eRuntimeMutable
  @DocProperty(description = "Enables SSO authentication")
  var enabled: Boolean = false

  @DocProperty(
    description =
      "Minutes after which the server will recheck the user's with the SSO provider to" +
        " ensure the user account is still valid. This is to prevent the user from being" +
        " able to access the server after the account has been disabled or deleted in the SSO provider.",
  )
  var sessionExpirationMinutes: Int = 10

  @DocProperty(
    description =
      "When enabled, per-organization SSO provider URLs (`tokenUri`, `authorizationUri`) may target " +
        "otherwise-blocked address ranges — loopback, private/site-local, link-local, IPv6 unique-local, " +
        "multicast and wildcard/any-local addresses. Useful when the identity provider runs on an internal " +
        "network in a self-hosted deployment.\n" +
        "\n" +
        ":::danger\n" +
        "This removes SSRF protection for SSO provider URLs. Keep it **disabled** on production and " +
        "multi-tenant servers — an organization owner able to configure SSO could otherwise make the server " +
        "reach internal services.\n" +
        ":::\n\n",
  )
  var allowLocalAddresses: Boolean = false
}
