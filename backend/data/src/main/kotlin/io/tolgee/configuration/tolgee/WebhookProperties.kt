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

  @DocProperty(
    description =
      "When enabled, webhook URLs may target otherwise-blocked address ranges — " +
        "loopback, private/site-local, link-local, IPv6 unique-local, multicast and " +
        "wildcard/any-local addresses. Useful for local development and integration testing.\n" +
        "\n" +
        ":::danger\n" +
        "This removes SSRF protection for webhook targets. Keep it **disabled** on production " +
        "and multi-tenant servers — anyone able to configure a webhook could otherwise reach " +
        "internal services.\n" +
        ":::\n\n",
  )
  var allowLocalAddresses: Boolean = false

  @PostConstruct
  fun validate() {
    require(autoDisableWarningAfterHours < autoDisableAfterDays * 24) {
      "tolgee.webhook.auto-disable-warning-after-hours ($autoDisableWarningAfterHours) " +
        "must be less than auto-disable-after-days ($autoDisableAfterDays) converted to hours (${autoDisableAfterDays * 24})"
    }
  }
}
