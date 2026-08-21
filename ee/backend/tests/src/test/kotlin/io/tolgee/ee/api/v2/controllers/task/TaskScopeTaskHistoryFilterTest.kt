package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.data.task.CalculateScopeRequest
import io.tolgee.ee.data.task.CreateMultipleTasksRequest
import io.tolgee.ee.data.task.CreateTaskRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TaskScopeTaskHistoryFilterTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TaskTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private lateinit var czechOnlyKey: KeyBuilder

  private val czechOnlyKeyId get() = czechOnlyKey.self.id

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    czechOnlyKey =
      testData.addKeyWithOwnTask(
        "czech only",
        number = 10,
        taskLanguage = testData.czechLanguage,
        state = TaskState.FINISHED,
      )
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun scopeRequest(languageId: Long) =
    CalculateScopeRequest(
      languageId = languageId,
      type = TaskType.TRANSLATE,
      keys = mutableSetOf(czechOnlyKeyId),
    )

  @Test
  @ProjectJWTAuthTestMethod
  fun `without the filter the key is in scope for both languages`() {
    performProjectAuthPost(
      "tasks/calculate-scope",
      scopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope",
      scopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterNeverInTask excludes the key only for the language it was tasked in`() {
    performProjectAuthPost(
      "tasks/calculate-scope?filterNeverInTask=true",
      scopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterNeverInTask=true",
      scopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterHasBeenInTask keeps the key only for the language it was tasked in`() {
    performProjectAuthPost(
      "tasks/calculate-scope?filterHasBeenInTask=true",
      scopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterHasBeenInTask=true",
      scopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creating tasks for several languages does not re-task the key in the language that already had it`() {
    val untouchedKeyId =
      testData.keysOutOfTask
        .first()
        .self.id
    val batchKeys = mutableSetOf(czechOnlyKeyId, untouchedKeyId)

    performProjectAuthPost(
      "tasks/create-multiple-tasks?filterNeverInTask=true",
      CreateMultipleTasksRequest(
        mutableSetOf(
          CreateTaskRequest(
            name = "EnglishBatch",
            type = TaskType.TRANSLATE,
            languageId = testData.englishLanguage.id,
            assignees = mutableSetOf(),
            keys = batchKeys,
          ),
          CreateTaskRequest(
            name = "CzechBatch",
            type = TaskType.TRANSLATE,
            languageId = testData.czechLanguage.id,
            assignees = mutableSetOf(),
            keys = batchKeys,
          ),
        ),
      ),
    ).andIsOk

    performProjectAuthGet("tasks?search=EnglishBatch").andIsOk.andAssertThatJson {
      node("_embedded.tasks[0].totalItems").isEqualTo(2)
    }

    performProjectAuthGet("tasks?search=CzechBatch").andIsOk.andAssertThatJson {
      node("_embedded.tasks[0].totalItems").isEqualTo(1)
    }
  }
}
