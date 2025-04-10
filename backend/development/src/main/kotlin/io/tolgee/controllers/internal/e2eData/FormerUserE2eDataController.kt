package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.FormerUserTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/former-user"])
@Transactional
class FormerUserE2eDataController : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  fun generateBasicTestData(): Map<String, Any> {
    val testData =
      FormerUserTestData().also {
        testDataService.saveTestData(it.root)
      }
    return mapOf("projectId" to testData.project.id)
  }

  override val testData: TestDataBuilder
    get() = FormerUserTestData().root
}
