package io.tolgee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.dtos.request.task.*
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.TaskType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TaskControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `task exists`() {
    performProjectAuthGet("tasks").andAssertThatJson {
      node("_embedded.tasks") {
        node("[0].id").isNumber
        node("[0].name").isEqualTo("New task")
      }
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(1))
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates new task`() {
    val keys = testData.keysInTask.map { it.self.id }.toMutableSet()
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
      node("id").isNumber
      node("name").isEqualTo("Another task")
      node("assignees[0].name").isEqualTo(testData.orgMember.self.name)
      node("language.tag").isEqualTo(testData.englishLanguage.tag)
      node("totalItems").isEqualTo(keys.size)
    }

    performProjectAuthGet("tasks").andAssertThatJson {
      node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
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
      "tasks/${testData.createdTask.self.id}",
      UpdateTaskRequest(
        name = "Updated task",
        description = "updated description",
        assignees = mutableSetOf(),
      ),
    ).andIsOk.andAssertThatJson {
      node("id").isEqualTo(testData.createdTask.self.id)
      node("name").isEqualTo("Updated task")
      node("description").isEqualTo("updated description")
      node("assignees").isEqualTo(mutableListOf<Any>())
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails when updating assignees to non-members`() {
    performProjectAuthPut(
      "tasks/${testData.createdTask.self.id}",
      UpdateTaskRequest(
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
      "tasks/${testData.createdTask.self.id}/keys",
      UpdateTaskKeysRequest(
        addKeys = testData.keysOutOfTask.map { it.self.id }.toMutableSet(),
      ),
    ).andIsOk

    performProjectAuthGet(
      "tasks/${testData.createdTask.self.id}",
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
      "tasks/${testData.createdTask.self.id}/keys",
      UpdateTaskKeysRequest(
        removeKeys = keysToRemove,
      ),
    ).andIsOk

    performProjectAuthGet(
      "tasks/${testData.createdTask.self.id}",
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
        language = testData.englishLanguage.id,
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
        language = testData.englishLanguage.id,
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
  fun `includes keys in included in task of different type`() {
    val allKeys = testData.keysInTask.map { it.self.id }.toMutableSet()
    performProjectAuthPost(
      "tasks/calculate-scope",
      CalculateScopeRequest(
        language = testData.englishLanguage.id,
        type = TaskType.REVIEW,
        keys = allKeys,
      ),
    ).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(allKeys.size)
      node("wordCount").isEqualTo(4)
      node("characterCount").isEqualTo(26)
    }
  }
}
