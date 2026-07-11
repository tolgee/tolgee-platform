package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationProtection
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@InternalController(["internal/e2e-data/suggestions"])
class SuggestionsE2eDataController(
  private val generatingService: TestDataGeneratingService,
) : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = SuggestionsTestData().root

  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(
    @RequestParam
    suggestionsMode: SuggestionsMode = SuggestionsMode.DISABLED,
    @RequestParam
    translationProtection: TranslationProtection = TranslationProtection.NONE,
    @RequestParam
    disableTranslation: Boolean = false,
  ): StandardTestDataResult {
    val data = SuggestionsTestData()
    data.projectBuilder.self.suggestionsMode = suggestionsMode
    data.projectBuilder.self.translationProtection = translationProtection
    if (disableTranslation) {
      data.disableTranslation()
    }
    testDataService.saveTestData(data.root)
    return generatingService.getStandardResult(data.root)
  }
}
