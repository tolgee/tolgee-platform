package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.LabelsTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/label"])
class LabelE2eDataController(
  private val generatingService: TestDataGeneratingService,
) : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(): StandardTestDataResult {
    val data = LabelsTestData()
    testDataService.saveTestData(data.root)
    return generatingService.getStandardResult(data.root)
  }

  override val testData: TestDataBuilder
    get() = LabelsTestData().root
}
