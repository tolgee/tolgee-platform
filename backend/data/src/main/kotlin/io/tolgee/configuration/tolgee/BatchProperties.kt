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
}
