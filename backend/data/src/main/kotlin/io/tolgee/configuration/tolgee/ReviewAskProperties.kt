package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.review-ask")
@DocProperty(
  description =
    "Once startup finishes, self-hosted instances print a short message asking for a review of Tolgee on G2.",
  displayName = "Review ask",
)
class ReviewAskProperties {
  @DocProperty(description = "Whether to print the review ask on startup")
  var enabled: Boolean = true
}
