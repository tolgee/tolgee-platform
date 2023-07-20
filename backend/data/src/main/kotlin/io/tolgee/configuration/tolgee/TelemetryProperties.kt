package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.telemetry")
@DocProperty(
  description = "Properties for telemetry. Tolgee sends anonymous data about usage to help us improve the product." +
    "\n\n" +
    "Once a day we collect following data" +
    "- number of projects\n" +
    "- number of languages\n" +
    "- number of translations\n" +
    "- number of users\n\n" +
    "We don't collect any other data. Please leave telemetry enabled to help us improve Tolgee.",
  displayName = "Telemetry"
)
class TelemetryProperties {
  @DocProperty(description = "Whether telemetry is enabled")
  var enabled: Boolean = true

  @DocProperty(hidden = true)
  var server: String = "https://app.tolgee.io"
}
