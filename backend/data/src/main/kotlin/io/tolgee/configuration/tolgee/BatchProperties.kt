package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.batch")
@DocProperty(description = "Configuration of batch operations.", displayName = "Batch operations")
class BatchProperties {
  @DocProperty(description = "How many parallel jobs can be run at once on single Tolgee instance")
  var concurrency: Int = 1

  @DocProperty(
    description =
      "Concurrency among all tolgee instances per one machine translation job\n." +
        "Higher concurrency provides faster and distributed processing, but can lead to hitting rate limit\n" +
        "on OpenAI, as well as getting over the limit of availableCredits: at the beginning, we only check, that\n" +
        "the organization has `availableCredits > 0` and only when we have the result from OpenAI,we are able to\n" +
        "calculate how many credits to charge the organization. In the Tolgee Cloud it is set to 1. ",
    defaultValue = "-1",
    defaultExplanation = "Unlimited (within tolgee.batch.concurrency)",
  )
  var maxPerMtJobConcurrency: Int = -1

  @DocProperty(description = "Enable scheduled cleanup of old batch jobs")
  var oldJobCleanupEnabled: Boolean = true

  @DocProperty(description = "Retention period in days for completed batch jobs (SUCCESS, CANCELLED)")
  var completedJobRetentionDays: Int = 3

  @DocProperty(description = "Retention period in days for failed batch jobs")
  var failedJobRetentionDays: Int = 30

  @DocProperty(
    description = "Delay between old batch job cleanup runs in milliseconds",
    defaultExplanation = "8 hours",
  )
  var oldJobCleanupDelayInMs: Long = 28800000

  @DocProperty(description = "Batch size for deleting old jobs (to avoid long-running transactions)")
  var jobCleanupBatchSize: Int = 1000

  @DocProperty(
    description =
      "Lock lease time in milliseconds for old job cleanup " +
        "(to prevent lock expiration during long cleanups)",
    defaultExplanation = "1 day",
  )
  var jobCleanupLockLeaseTimeMs: Long = 86400000

  @DocProperty(
    description =
      "Timeout in milliseconds to wait for running job chunks to complete when cancelling a batch job. " +
        "AI translation operations can take longer, so this should be set high enough to accommodate them.",
    defaultExplanation = "30 seconds",
  )
  var cancellationTimeoutMs: Long = 30000

  @DocProperty(description = "Poll interval in seconds for checking OpenAI Batch API status")
  var batchApiPollIntervalSeconds: Int = 60

  @DocProperty(description = "Maximum hours to wait for an OpenAI batch to complete before timing out")
  var batchApiMaxWaitHours: Int = 24

  @DocProperty(description = "Number of translation items per OpenAI batch file chunk")
  var batchApiChunkSize: Int = 5000

  @DocProperty(description = "Maximum number of items allowed in a single batch API job")
  var batchApiMaxItemsPerJob: Int = 50000

  @DocProperty(description = "Maximum concurrent OpenAI batch API jobs per organization")
  var batchApiMaxConcurrentPerOrg: Int = 5

  @DocProperty(description = "Maximum concurrent OpenAI batch API jobs globally")
  var batchApiMaxConcurrentGlobal: Int = 100

  @DocProperty(description = "Maximum number of OpenAI batch statuses to poll in a single poll cycle")
  var batchApiMaxPollBatchSize: Int = 50
}
