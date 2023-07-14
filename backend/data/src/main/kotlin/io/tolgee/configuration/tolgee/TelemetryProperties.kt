package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.telemetry")
@DocProperty(
  description = "Properties for telemetry. Tolgee sends anonymous data about usage to help us improve the product.",
  displayName = "Telemetry"
)
class TelemetryProperties {
  @DocProperty(description = "Whether telemetry is enabled")
  var enabled: Boolean = true

  @DocProperty(hidden = true)
  var server: String = "https://app.tolgee.io"
}
