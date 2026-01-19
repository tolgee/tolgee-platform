package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.batch")
@DocProperty(description = "Configuration of batch operations.", displayName = "Batch operations")
class BatchProperties {
  @DocProperty(description = "How many parallel jobs can be run at once on single Tolgee instance")
  var concurrency: Int = 1

  @DocProperty(description = "How many job chunks are added to the internal queue on each scheduled run")
  var chunkQueuePopulationSize: Int = 1_000

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
}
