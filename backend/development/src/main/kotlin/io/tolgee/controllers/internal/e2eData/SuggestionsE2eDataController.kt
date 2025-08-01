package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.model.enums.SuggestionsMode
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/suggestions"])
@Transactional
class SuggestionsE2eDataController(
  private val generatingService: TestDataGeneratingService,
) : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = SuggestionsTestData().root

  @GetMapping(value = ["/generate-suggestions-enabled"])
  @Transactional
  fun generateBasicTestData(): StandardTestDataResult {
    val data = SuggestionsTestData()
    data.projectBuilder.self.suggestionsMode = SuggestionsMode.ENABLED
    testDataService.saveTestData(data.root)
    return generatingService.getStandardResult(data.root)
  }
}
