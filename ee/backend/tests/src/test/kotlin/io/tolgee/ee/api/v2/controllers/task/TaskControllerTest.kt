package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.task.*
import io.tolgee.fixtures.*
import io.tolgee.model.enums.TaskType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class TaskControllerTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `creates new task`() {
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
            testData.orgMember.self.id,
          ),
        keys = keys,
      ),
    ).andAssertThatJson {
      node("number").isNumber
      node("name").isEqualTo("Another task")
      node("assignees[0].name").isEqualTo(testData.orgMember.self.name)
      node("language.tag").isEqualTo(testData.englishLanguage.tag)
      node("totalItems").isEqualTo(keys.size)
    }

    performProjectAuthGet("tasks").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(3))
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
  fun `close and reopen task`() {
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/close",
    ).andIsOk.andAssertThatJson {
      node("state").isEqualTo("CLOSED")
    }
    performProjectAuthPut(
      "tasks/${testData.translateTask.self.number}/reopen",
    ).andIsOk.andAssertThatJson {
      node("state").isEqualTo("NEW")
    }
  }
}
