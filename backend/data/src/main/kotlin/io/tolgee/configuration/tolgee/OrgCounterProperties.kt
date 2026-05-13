package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "tolgee.org-counter")
@DocProperty(
  description = "Configuration for the per-organization usage counter that tracks key and translation counts.",
  displayName = "Organization usage counter",
)
class OrgCounterProperties {
  @DocProperty(
    description =
      "Master kill-switch. When false, the counter is bypassed and the platform falls back " +
        "to the original recount-on-every-commit behavior. Use to disable in a hurry if the " +
        "counter misbehaves.",
  )
  var enabled: Boolean = true

  @DocProperty(
    description =
      "Trigger a definitive recount via OrganizationStatsService when the counter says " +
        "`count >= threshold * limit`. A drifted counter near the limit never causes a wrongful " +
        "400 because the recount is the final word. Values: 0.0–1.0.",
  )
  var boundaryVerifyThreshold: Double = 0.95

  @DocProperty(
    description = "Cron expression for the nightly reconciliation job. Defaults to 03:00 daily.",
  )
  var reconciliationCron: String = "0 0 3 * * *"

  @DocProperty(description = "Orgs reconciled per batch.")
  var reconciliationPageSize: Int = 100

  @DocProperty(
    description =
      "Max runtime per reconciliation cycle. The remainder resumes on the next cycle " +
        "(ordered by least-recently reconciled).",
  )
  var reconciliationMaxDuration: Duration = Duration.ofMinutes(30)
}
