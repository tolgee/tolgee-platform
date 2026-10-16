package io.tolgee.controllers.internal.e2eData

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.FeatureFlag
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AiReviewE2eTestData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam

@InternalController(["internal/e2e-data/ai-review"])
class AiReviewE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var tolgeeProperties: TolgeeProperties

  override val testData: TestDataBuilder
    get() = AiReviewE2eTestData().root

  @PutMapping(value = ["/flag-enabled-for-all"])
  fun setFlagEnabledForAll(
    @RequestParam enabled: Boolean,
  ) {
    tolgeeProperties.featureFlags.enabledForAll = getEnabledForAll(enabled)
  }

  private fun getEnabledForAll(enabled: Boolean): List<FeatureFlag> {
    if (!enabled) return emptyList()
    return listOf(FeatureFlag.AI_REVIEW)
  }
}
