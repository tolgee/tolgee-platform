package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.TestEmailConfiguration
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.task.CalculateScopeRequest
import io.tolgee.ee.data.task.CreateMultipleTasksRequest
import io.tolgee.ee.data.task.CreateTaskRequest
import io.tolgee.ee.data.task.UpdateTaskKeyRequest
import io.tolgee.ee.data.task.UpdateTaskKeysRequest
import io.tolgee.ee.data.task.UpdateTaskRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.enums.TaskType
import io.tolgee.model.notifications.NotificationType.TASK_CANCELED
import io.tolgee.model.notifications.NotificationType.TASK_FINISHED
import io.tolgee.model.task.TaskKey
import io.tolgee.repository.TaskKeyRepository
import io.tolgee.testing.NotificationTestUtil
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@Import(TestEmailConfiguration::class)
class TaskControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var taskKeyRepository: TaskKeyRepository

  @Autowired
  private lateinit var notificationUtil: NotificationTestUtil

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
    notificationUtil.init()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of tasks`() {
    performProjectAuthGet("tasks").andAssertThatJson {
      node("_embedded.tasks") {
        node("[0].number").isEqualTo(1)
        node("[0].name").isEqualTo("Translate task")
        node("[1].number").isEqualTo(2)
        node("[1].name").isEqualTo("Review task")
      }
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `gets task detail`() {
    performProjectAuthGet(
      "tasks/${testData.translateTask.self.number}",
    ).andAssertThatJson {
      node("name").isEqualTo("Translate task")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates new task which triggers notification`() {
    val keys = testData.keysOutOfTask.map { it.self.id }.toMutableSet()
    performProjectAuthPost(
      "tasks",
      CreateTaskRequest(
        name = "Another task",
        description = "...",
        type = TaskType.TRANSLATE,
        languageId = testData.englishLanguage.id,
        assignees =
          mutableSetOf(
            testData.user.id,
          ),
        keys = keys,
      ),
    ).andAssertThatJson {
      node("number").isNumber
      node("name").isEqualTo("Another task")
      node("assignees[0].name").isEqualTo(testData.user.name)
      node("language.tag").isEqualTo(testData.englishLanguage.tag)
      node("totalItems").isEqualTo(keys.size)
    }

    performProjectAuthGet("tasks").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(3))
    }

    executeInNewTransaction {
      assertThat(notificationUtil.newestInAppNotification().linkedTask?.name).isEqualTo("Another task")
      waitForNotThrowing(timeout = 2000, pollTime = 25) {
        assertThat(
          notificationUtil.newestEmailNotification(),
        ).contains("/projects/${testData.project.id}/task?number=3")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates multiple new tasks`() {
    val keys = testData.keysOutOfTask.map { it.self.id }.toMutableSet()
    performProjectAuthPost(
      "tasks/create-multiple-tasks",
      CreateMultipleTasksRequest(
        mutableSetOf(
          CreateTaskRequest(
            name = "Another task",
            description = "...",
            type = TaskType.TRANSLATE,
            languageId = testData.englishLanguage.id,
            assignees =
              mutableSetOf(
                testData.orgMember.self.id,
              ),
            keys = keys,
          ),
          CreateTaskRequest(
            name = "Another task",
            description = "...",
            type = TaskType.TRANSLATE,
            languageId = testData.czechLanguage.id,
            assignees =
              mutableSetOf(
                testData.orgMember.self.id,
              ),
            keys = keys,
          ),
        ),
      ),
    ).andIsOk

    performProjectAuthGet("tasks").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(4))
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `calculates stats for task`() {
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/keys/${testData.keysInTask.first().self.id}",
      UpdateTaskKeyRequest(done = true),
    ).andIsOk

    performProjectAuthGet(
      "tasks/${testData.translateTask.self.number}",
    ).andIsOk.andAssertThatJson {
      node("totalItems").isEqualTo(2)
      node("doneItems").isEqualTo(1)
    }

    performProjectAuthGet(
      "tasks/${testData.translateTask.self.number}/per-user-report",
    ).andIsOk.andAssertThatJson {
      node("[0]").node("doneItems").isEqualTo(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails to create task with assignee from different project`() {
    performProjectAuthPost(
      "tasks",
      CreateTaskRequest(
        name = "Another task",
        description = "...",
        type = TaskType.TRANSLATE,
        languageId = testData.englishLanguage.id,
        assignees =
          mutableSetOf(
            testData.unrelatedUser.self.id,
          ),
        keys = mutableSetOf(),
      ),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo(Message.USER_HAS_NO_PROJECT_ACCESS)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails to create task with language from different project`() {
    performProjectAuthPost(
      "tasks",
      CreateTaskRequest(
        name = "Another task",
        description = "...",
        type = TaskType.TRANSLATE,
        languageId = testData.unrelatedEnglish.self.id,
        assignees = mutableSetOf(),
        keys = mutableSetOf(),
      ),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo(Message.LANGUAGE_NOT_FROM_PROJECT)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates existing task`() {
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}",
      UpdateTaskRequest(
        name = "Updated task",
        description = "updated description",
        assignees = mutableSetOf(),
      ),
    ).andIsOk.andAssertThatJson {
      node("number").isEqualTo(testData.translateTask.self.number)
      node("name").isEqualTo("Updated task")
      node("description").isEqualTo("updated description")
      node("assignees").isEqualTo(mutableListOf<Any>())
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails when updating assignees to non-members`() {
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}",
      UpdateTaskRequest(
        name = testData.translateTask.self.name,
        assignees = mutableSetOf(testData.unrelatedUser.self.id),
      ),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo(Message.USER_HAS_NO_PROJECT_ACCESS)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can add keys`() {
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/keys",
      UpdateTaskKeysRequest(
        addKeys = testData.keysOutOfTask.map { it.self.id }.toMutableSet(),
      ),
    ).andIsOk

    performProjectAuthGet(
      "tasks/${testData.translateTask.self.number}",
    ).andIsOk.andAssertThatJson {
      // Calculate expected keys set
      val expectedItems = (testData.keysInTask union testData.keysOutOfTask).size

      node("totalItems").isEqualTo(expectedItems)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `can remove keys`() {
    val allKeys = testData.keysInTask.map { it.self.id }.toMutableSet()
    val keysToRemove = mutableSetOf(allKeys.first())
    val remainingKeys = allKeys.subtract(keysToRemove)
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/keys",
      UpdateTaskKeysRequest(
        removeKeys = keysToRemove,
      ),
    ).andIsOk

    performProjectAuthGet(
      "tasks/${testData.translateTask.self.number}",
    ).andIsOk.andAssertThatJson {
      node("totalItems").isEqualTo(remainingKeys.size)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `calculates stats`() {
    val allKeys = testData.keysOutOfTask.map { it.self.id }.toMutableSet()
    performProjectAuthPost(
      "tasks/calculate-scope",
      CalculateScopeRequest(
        languageId = testData.englishLanguage.id,
        type = TaskType.TRANSLATE,
        keys = allKeys,
      ),
    ).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(allKeys.size)
      node("wordCount").isEqualTo(4)
      node("characterCount").isEqualTo(26)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't include keys which are in different task of same type`() {
    val allKeys = testData.keysInTask.map { it.self.id }.toMutableSet()
    performProjectAuthPost(
      "tasks/calculate-scope",
      CalculateScopeRequest(
        languageId = testData.englishLanguage.id,
        type = TaskType.TRANSLATE,
        keys = allKeys,
      ),
    ).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(0)
      node("wordCount").isEqualTo(0)
      node("characterCount").isEqualTo(0)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `includes keys included in task of different type`() {
    val allKeys = testData.keysInTask.map { it.self.id }.toMutableSet()
    performProjectAuthPost(
      "tasks/calculate-scope",
      CalculateScopeRequest(
        languageId = testData.englishLanguage.id,
        type = TaskType.REVIEW,
        keys = allKeys,
      ),
    ).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(allKeys.size)
      node("wordCount").isEqualTo(4)
      node("characterCount").isEqualTo(26)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project tasks`() {
    performProjectAuthGet(
      "tasks",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project tasks filter by assignee`() {
    performProjectAuthGet(
      "tasks?filterAssignee=${testData.orgMember.self.id}",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(1)
      node("_embedded.tasks[0].name").isEqualTo("Review task")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `project tasks filter by type`() {
    performProjectAuthGet(
      "tasks?filterType=${TaskType.TRANSLATE}",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(1)
      node("_embedded.tasks[0].name").isEqualTo("Translate task")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cancel, reopen and finish task, check notifications`() {
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/cancel",
    ).andIsOk.andAssertThatJson {
      node("state").isEqualTo("CANCELED")
    }
    notificationUtil.newestInAppNotification().also {
      assertThat(it.type).isEqualTo(TASK_CANCELED)
      assertThat(it.linkedTask?.id).isEqualTo(testData.translateTask.self.id)
    }
    assertThat(notificationUtil.newestEmailNotification()).contains("has been canceled")
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/reopen",
    ).andIsOk.andAssertThatJson {
      node("state").isEqualTo("NEW")
    }
    testData.translateTaskKeys.markAllKeysDone()
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/finish",
    ).andIsOk.andAssertThatJson {
      node("state").isEqualTo("FINISHED")
    }
    notificationUtil.newestInAppNotification().also {
      assertThat(it.type).isEqualTo(TASK_FINISHED)
      assertThat(it.linkedTask?.id).isEqualTo(testData.translateTask.self.id)
    }
    assertThat(notificationUtil.newestEmailNotification()).contains("has been finished")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `canceled tasks can be filtered out by timestamp`() {
    val timeBeforeCreation = System.currentTimeMillis()
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/cancel",
    ).andIsOk.andAssertThatJson {
      node("state").isEqualTo("CANCELED")
    }
    val timeAfterCreation = System.currentTimeMillis()

    // should be included
    performProjectAuthGet(
      "tasks?filterNotClosedBefore=$timeBeforeCreation",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(2)
      node("_embedded.tasks[0].name").isEqualTo("Translate task")
    }

    // should be excluded
    performProjectAuthGet(
      "tasks?filterNotClosedBefore=$timeAfterCreation",
    ).andIsOk.andAssertThatJson {
      node("page").node("totalElements").isEqualTo(1)
      node("_embedded.tasks[0].name").isEqualTo("Review task")
    }
  }

  private fun Collection<TaskKey>.markAllKeysDone() =
    forEach { key ->
      key.done = true
      taskKeyRepository.save(key)
    }
}
