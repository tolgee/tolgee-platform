package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/websocket-translations"])
class WebsocketTranslationsE2eDataController : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateKeys(): Map<String, Long> {
    val testData = TranslationsTestData()
    testDataService.saveTestData(testData.root)
    return mapOf("keyId" to testData.aKey.id, "projectId" to testData.project.id)
  }

  override val testData: TestDataBuilder
    get() = TranslationsTestData().root
}
