package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TaskTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/task"])
@Transactional
class TaskE2eDataController() : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData() {
    val data = TaskTestData()
    testDataService.saveTestData(data.root)
  }

  override val testData: TestDataBuilder
    get() = TaskTestData().root
}
