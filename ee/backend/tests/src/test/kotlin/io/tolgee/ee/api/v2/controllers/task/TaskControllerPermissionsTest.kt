package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.task.UpdateTaskKeyRequest
import io.tolgee.ee.data.task.UpdateTaskKeysRequest
import io.tolgee.ee.data.task.UpdateTaskRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TaskControllerPermissionsTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sees only tasks assigned to him`() {
    // is assigned to translate task
    userAccount = testData.projectUser.self

    performAuthGet(
      "/v2/user-tasks",
    ).andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(1)
      node("_embedded.tasks[0].name").isEqualTo("Translate task")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can access assigned task`() {
    userAccount = testData.projectUser.self

    performProjectAuthGet("tasks/${testData.translateTask.self.number}").andIsOk
    performProjectAuthGet("tasks/${testData.translateTask.self.number}/per-user-report").andIsOk
    performProjectAuthGet("tasks/${testData.translateTask.self.number}/xlsx-report").andIsOk
    performProjectAuthGet("tasks/${testData.translateTask.self.number}/keys").andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can do assigned task's basic operations`() {
    userAccount = testData.projectUser.self

    testData.keysInTask.forEach {
      performProjectAuthPut(
        "tasks/${testData.translateTask.self.number}/keys/${it.self.id}",
        UpdateTaskKeyRequest(done = true),
      ).andIsOk
    }
    performProjectAuthPut("tasks/${testData.translateTask.self.number}/finish").andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can't do advanced assigned task's operations`() {
    userAccount = testData.projectUser.self

    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/keys",
      UpdateTaskKeysRequest(
        addKeys =
          mutableSetOf(
            testData.keysOutOfTask
              .first()
              .self.id,
          ),
      ),
    ).andIsForbidden
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}",
      UpdateTaskRequest(name = "Test"),
    ).andIsForbidden
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/close",
    ).andIsForbidden
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/reopen",
    ).andIsForbidden
  }
}
