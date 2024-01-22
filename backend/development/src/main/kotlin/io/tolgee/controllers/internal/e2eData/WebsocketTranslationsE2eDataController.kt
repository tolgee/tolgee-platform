package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/websocket-translations"])
@Transactional
class WebsocketTranslationsE2eDataController() : AbstractE2eDataController() {
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
