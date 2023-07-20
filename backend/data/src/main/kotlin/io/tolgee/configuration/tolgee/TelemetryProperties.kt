package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.telemetry")
@DocProperty(
  description = """By default, self-hosted instances send anonymous data about usage to help us improve Tolgee.

:::info
This was added in Tolgee Platform v3.23.0
:::

Once a day we collect following data
- number of projects
- number of languages
- number of translations
- number of users

We don't collect any other data. Please leave telemetry enabled to help us improve Tolgee.""",
  displayName = "Telemetry"
)
class TelemetryProperties {
  @DocProperty(description = "Whether telemetry is enabled")
  var enabled: Boolean = true

  @DocProperty(hidden = true)
  var server: String = "https://app.tolgee.io"
}
