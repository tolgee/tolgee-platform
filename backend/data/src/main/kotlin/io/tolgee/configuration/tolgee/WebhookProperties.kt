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
    description = "Number of hours of continuous failure before a warning email is sent.",
  )
  var autoDisableWarningAfterHours: Int = 6

  @DocProperty(
    description = "Number of days of continuous failure before a webhook is automatically disabled.",
  )
  var autoDisableAfterDays: Int = 3

  @PostConstruct
  fun validate() {
    require(autoDisableWarningAfterHours < autoDisableAfterDays * 24) {
      "tolgee.webhook.auto-disable-warning-after-hours ($autoDisableWarningAfterHours) " +
        "must be less than auto-disable-after-days ($autoDisableAfterDays) converted to hours (${autoDisableAfterDays * 24})"
    }
  }
}
