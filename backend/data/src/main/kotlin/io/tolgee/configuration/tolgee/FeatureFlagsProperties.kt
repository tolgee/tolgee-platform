package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.constants.FeatureFlag
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.feature-flags")
@DocProperty(
  description =
    "Feature flags hide features that are not generally available yet. " +
      "Server administrators can enable them per organization in the administration.",
  displayName = "Feature flags",
)
class FeatureFlagsProperties(
  @DocProperty(description = "Feature flags enabled for all organizations.")
  var enabledForAll: List<FeatureFlag> = emptyList(),
)
