package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AvatarsTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/avatars"])
class AvatarsE2eDataController : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(): Map<String, *> {
    val data = AvatarsTestData()
    testDataService.saveTestData(data.root)
    return mapOf(
      "projectId" to data.projectBuilder.self.id,
      "organizationSlug" to data.organization.slug,
    )
  }

  override val testData: TestDataBuilder
    get() = AvatarsTestData().root
}
