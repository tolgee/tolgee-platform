package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.sentry")
@DocProperty(
  description = "Tolgee uses [Sentry](https://sentry.io/) for error reporting.",
  displayName = "Sentry",
)
class SentryProperties {
  @DocProperty(description = "Server DSN. If unset, error reporting is disabled on the server.")
  var serverDsn: String? = null

  @DocProperty(description = "Client DSN. If unset, error reporting is disabled on the server.")
  var clientDsn: String? = null

  @DocProperty(description = "Sample rate for Sentry traces. If unset, traces are disabled on the server.")
  var tracesSampleRate: Double? = null
}
