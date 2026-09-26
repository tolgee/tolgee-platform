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
  private lateinit var openReviewKey: KeyBuilder
  private lateinit var reviewOnlyKey: KeyBuilder

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
    openReviewKey =
      testData.addKeyWithOwnTask(
        "open review",
        number = 11,
        taskLanguage = testData.englishLanguage,
        type = TaskType.REVIEW,
        state = TaskState.IN_PROGRESS,
      )
    reviewOnlyKey =
      testData.addKeyWithOwnTask(
        "review only",
        number = 12,
        taskLanguage = testData.englishLanguage,
        type = TaskType.REVIEW,
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

  private fun openReviewAndFreeKeyRequest() =
    CalculateScopeRequest(
      languageId = testData.englishLanguage.id,
      type = TaskType.REVIEW,
      keys =
        mutableSetOf(
          openReviewKey.self.id,
          testData.keysOutOfTask
            .first()
            .self.id,
        ),
    )

  private fun czechOnlyKeyScopeRequest(languageId: Long) =
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
      czechOnlyKeyScopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope",
      czechOnlyKeyScopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterNeverInTask excludes the key only for the language it was tasked in`() {
    performProjectAuthPost(
      "tasks/calculate-scope?filterNeverInTask=true",
      czechOnlyKeyScopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterNeverInTask=true",
      czechOnlyKeyScopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterHasBeenInTask keeps the key only for the language it was tasked in`() {
    performProjectAuthPost(
      "tasks/calculate-scope?filterHasBeenInTask=true",
      czechOnlyKeyScopeRequest(testData.czechLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterHasBeenInTask=true",
      czechOnlyKeyScopeRequest(testData.englishLanguage.id),
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterNotInOpenTask drops the key only for the type that has it open`() {
    val request =
      CalculateScopeRequest(
        languageId = testData.englishLanguage.id,
        type = TaskType.TRANSLATE,
        keys = mutableSetOf(openReviewKey.self.id),
      )

    performProjectAuthPost(
      "tasks/calculate-scope?filterNotInOpenTask=true&filterTaskType=TRANSLATE",
      request,
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterNotInOpenTask=true&filterTaskType=REVIEW",
      request,
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `filterNeverInTask narrows to the given task type`() {
    val request =
      CalculateScopeRequest(
        languageId = testData.englishLanguage.id,
        type = TaskType.TRANSLATE,
        keys = mutableSetOf(reviewOnlyKey.self.id),
      )

    performProjectAuthPost(
      "tasks/calculate-scope?filterNeverInTask=true&filterTaskType=TRANSLATE",
      request,
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(1) }

    performProjectAuthPost(
      "tasks/calculate-scope?filterNeverInTask=true&filterTaskType=TRANSLATE&filterTaskType=REVIEW",
      request,
    ).andIsOk.andAssertThatJson { node("keyCount").isEqualTo(0) }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `under the creation defaults the preview count matches what creation will produce`() {
    performProjectAuthPost(
      "tasks/calculate-scope?filterNotInOpenTask=true&filterTaskType=REVIEW",
      openReviewAndFreeKeyRequest(),
    ).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(1)
      node("keyCountIncludingConflicts").isEqualTo(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `without the status filter the preview counts keys creation will drop`() {
    performProjectAuthPost(
      "tasks/calculate-scope",
      openReviewAndFreeKeyRequest(),
    ).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(1)
      node("keyCountIncludingConflicts").isEqualTo(2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a history filter narrows the conflict count too`() {
    val request =
      CalculateScopeRequest(
        languageId = testData.czechLanguage.id,
        type = TaskType.TRANSLATE,
        keys =
          mutableSetOf(
            czechOnlyKeyId,
            testData.keysOutOfTask
              .first()
              .self.id,
          ),
      )

    performProjectAuthPost("tasks/calculate-scope?filterNeverInTask=true", request).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(1)
      node("keyCountIncludingConflicts").isEqualTo(1)
    }

    performProjectAuthPost("tasks/calculate-scope", request).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(2)
      node("keyCountIncludingConflicts").isEqualTo(2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a type scope that excludes the created type still counts the conflict`() {
    val request =
      CalculateScopeRequest(
        languageId = testData.englishLanguage.id,
        type = TaskType.TRANSLATE,
        keys =
          mutableSetOf(
            testData.keysInTask
              .first()
              .self.id,
          ),
      )

    performProjectAuthPost(
      "tasks/calculate-scope?filterNotInOpenTask=true&filterTaskType=REVIEW",
      request,
    ).andIsOk.andAssertThatJson {
      node("keyCount").isEqualTo(0)
      node("keyCountIncludingConflicts").isEqualTo(1)
    }
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
