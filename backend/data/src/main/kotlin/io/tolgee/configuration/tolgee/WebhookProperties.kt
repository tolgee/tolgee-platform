package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.webhook")
@DocProperty(description = "Configuration for webhook behavior.", displayName = "Webhooks")
class WebhookProperties {
  @DocProperty(
    description = "Whether the automatic disabling of failing webhooks is enabled.",
  )
  var autoDisableEnabled: Boolean = true

  @DocProperty(
    description = "How often (in milliseconds) to check for webhooks to auto-disable.",
    defaultExplanation = "= 1 hour",
  )
  var autoDisableCheckPeriodMs: Long = 3_600_000

  @DocProperty(
    description = "Number of hours of continuous failure before a warning email is sent.",
  )
  var autoDisableWarningAfterHours: Int = 6

  @DocProperty(
    description = "Number of days of continuous failure before a webhook is automatically disabled.",
  )
  var autoDisableAfterDays: Int = 3

  @DocProperty(
    description = "Lease time (in milliseconds) for the distributed lock used by the auto-disable job.",
    defaultExplanation = "= 10 minutes",
  )
  var autoDisableLockLeaseTimeMs: Long = 600_000

  @PostConstruct
  fun validate() {
    require(autoDisableWarningAfterHours < autoDisableAfterDays * 24) {
      "tolgee.webhook.auto-disable-warning-after-hours ($autoDisableWarningAfterHours) " +
        "must be less than auto-disable-after-days ($autoDisableAfterDays) converted to hours (${autoDisableAfterDays * 24})"
    }
  }
}
