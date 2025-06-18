package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.LabelsTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/label"])
@Transactional
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
