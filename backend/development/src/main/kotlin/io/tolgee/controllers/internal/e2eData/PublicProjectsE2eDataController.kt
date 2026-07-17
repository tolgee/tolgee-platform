package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.PublicProjectsE2eData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/public-projects"])
class PublicProjectsE2eDataController(
  private val generatingService: TestDataGeneratingService,
) : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = PublicProjectsE2eData().root

  @GetMapping(value = ["/generate-few"])
  @Transactional
  fun generateFew(): StandardTestDataResult {
    return generatingService.generate(PublicProjectsE2eData(count = 5).root)
  }
}
