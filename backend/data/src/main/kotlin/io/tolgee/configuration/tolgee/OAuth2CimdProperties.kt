package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.oauth2.cimd")
@DocProperty(
  description =
    "Client ID Metadata Documents (CIMD): lets OAuth clients Tolgee has never seen — typically MCP clients — " +
      "register by presenting an HTTPS URL as their client_id. Active whenever the instance's issuer " +
      "(tolgee.back-end-url) is configured.",
  displayName = "OAuth2 CIMD",
)
class OAuth2CimdProperties {
  @DocProperty(
    description =
      "Hosts allowed to serve client metadata documents. Empty (the default) allows any public host; the " +
        "outbound fetch is SSRF-guarded and DNS-pinned either way. Set this to restrict which apps can ask " +
        "your users for access — or to a value matching nothing to turn CIMD off.",
    defaultValue = "",
  )
  var allowedHosts: List<String> = listOf()

  @DocProperty(description = "Timeout for fetching a client metadata document, in milliseconds.")
  var fetchTimeoutMs: Long = 5000

  @DocProperty(description = "Maximum size of a client metadata document, in bytes.")
  var maxDocumentBytes: Long = 16384

  @DocProperty(description = "How long a resolved client metadata document is cached, in seconds.")
  var cacheTtlSeconds: Long = 300

  @DocProperty(description = "How long a failed resolution is cached before the URL is fetched again, in seconds.")
  var negativeCacheTtlSeconds: Long = 60
}
