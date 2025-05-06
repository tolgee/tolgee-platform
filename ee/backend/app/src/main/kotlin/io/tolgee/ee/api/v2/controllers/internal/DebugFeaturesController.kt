package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.exceptions.BadRequestException
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/features"])
@Transactional
class DebugFeaturesController(
  val enabledFeaturesProvider: EnabledFeaturesProvider,
) {
  @PutMapping(value = ["/toggle"])
  @Transactional
  fun toggleFeature(
    feature: Feature,
    @Schema(
      description =
        "If true, feature will be enabled, if false, disabled, " +
          "if null features will behave as if they were not set",
    )
    enabled: Boolean?,
  ) {
    val provider =
      enabledFeaturesProvider as? PublicEnabledFeaturesProvider
        ?: throw BadRequestException(
          "EnabledFeaturesProvider is not PublicEnabledFeaturesProvider. " +
            "Disable billing module to use this endpoint.",
        )

    if (enabled == null) {
      provider.forceEnabled = null
      return
    }

    if (enabled) {
      provider.forceEnabled = provider.forceEnabled?.plus(feature) ?: setOf(feature)
    } else {
      provider.forceEnabled = provider.forceEnabled?.minus(feature)
    }
  }
}
