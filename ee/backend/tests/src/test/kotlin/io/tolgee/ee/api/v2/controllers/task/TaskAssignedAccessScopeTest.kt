package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TasksAssignedAccessTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * The task-assignee elevation is gated on `Scope.TASKS_ASSIGNED_ACCESS`. These tests are about the granular-permission
 * population: role presets grant the scope by expansion, hand-picked scope sets only carry it if it was granted or
 * backfilled.
 */
class TaskAssignedAccessScopeTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TasksAssignedAccessTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = TasksAssignedAccessTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a granular permission without tasks assigned-access loses the assignee elevation`() {
    userAccount = testData.withoutScope

    finishTask().andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a granular permission carrying tasks assigned-access keeps the assignee elevation`() {
    userAccount = testData.withScope

    // The same request the other user is refused: the scope is the only thing that differs between them.
    finishTask().andIsOk
  }

  private fun finishTask() = performProjectAuthPut("tasks/${TasksAssignedAccessTestData.TASK_NUMBER}/finish")
}
