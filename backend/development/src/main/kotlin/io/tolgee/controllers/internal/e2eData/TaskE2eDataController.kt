package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/task"])
class TaskE2eDataController(
  private val generatingService: TestDataGeneratingService,
) : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(): StandardTestDataResult {
    val data = TaskTestData()
    data.addBlockedTask()
    data.addTaskInState("Canceled review task", TaskState.CANCELED, TaskType.REVIEW, 4)
    data.addTaskInState("Finished review task", TaskState.FINISHED, TaskType.REVIEW, 5)
    testDataService.saveTestData(data.root)
    return generatingService.getStandardResult(data.root)
  }

  override val testData: TestDataBuilder
    get() = TaskTestData().root
}
