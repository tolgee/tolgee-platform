package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.notifications")
@DocProperty(description = "Configuration of user notifications.", displayName = "Notifications")
class NotificationsProperties {
  @DocProperty(description = "Max number of linked entities stored per notification")
  var entityCap: Int = 100

  @DocProperty(description = "How long after a mention the mention email is sent, if still unseen, in ms")
  var mentionEmailDelayMs: Long = 300_000

  @DocProperty(description = "Work notifications are emailed after this long without new changes, in ms")
  var workQuietMs: Long = 900_000

  @DocProperty(description = "Work notifications are emailed at most this long after the item was created, in ms")
  var workMaxWaitMs: Long = 3_600_000

  @DocProperty(description = "Minimum time between two work emails for one user, in ms")
  var workEmailMinIntervalMs: Long = 14_400_000

  @DocProperty(description = "Minimum time between two work emails for users who chose a daily email, in ms")
  var dailyWorkEmailMinIntervalMs: Long = 86_400_000

  @DocProperty(description = "Delay between runs of the notification email job, in ms")
  var emailJobIntervalMs: Long = 60_000

  @DocProperty(description = "Whether the notification email and cleanup jobs are scheduled")
  var schedulingEnabled: Boolean = true

  @DocProperty(
    description =
      "Seen notifications, and email-only notifications that were never emailed, older than this are deleted, in days",
  )
  var seenRetentionDays: Long = 7

  @DocProperty(description = "Delay between runs of the cleanup job, in ms")
  var cleanupIntervalMs: Long = 3_600_000
}
