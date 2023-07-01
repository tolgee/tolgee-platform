package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.batch")
class BatchProperties {
  @DocProperty(description = "How many parallel jobs can be run at once on single Tolgee instance")
  var concurrency: Int = 10
}
