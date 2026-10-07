package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.notifications")
@DocProperty(description = "Configuration of user notifications.", displayName = "Notifications")
class NotificationsProperties {
  @DocProperty(description = "Delay between runs of the job that creates notifications from activity, in ms")
  var processingIntervalMs: Long = 30_000

  @DocProperty(description = "How many activity markers the job reads at once")
  var processingBatchSize: Int = 500

  @DocProperty(description = "How many times a failing activity marker is retried before it is dropped")
  var maxAttempts: Int = 5

  @DocProperty(description = "Max number of linked entities stored per notification")
  var entityCap: Int = 100

  @DocProperty(description = "Delay between runs of the digest email job, in ms")
  var digestIntervalMs: Long = 300_000

  @DocProperty(description = "Minimum age of the newest pending notification before a digest is sent, in ms")
  var digestGracePeriodMs: Long = 600_000

  @DocProperty(description = "Minimum time between two digest emails for one user, in hours")
  var digestPeriodHours: Long = 24

  @DocProperty(description = "Seen notifications older than this are deleted, in days")
  var seenRetentionDays: Long = 7

  @DocProperty(description = "Delay between runs of the cleanup job, in ms")
  var cleanupIntervalMs: Long = 3_600_000
}
