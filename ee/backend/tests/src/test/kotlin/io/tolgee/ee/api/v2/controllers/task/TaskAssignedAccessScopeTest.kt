package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TasksAssignedAccessTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsCreated
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
    enabledFeaturesProvider.forceEnabled = null
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `the assignee elevation on a translation in the task needs the scope too`() {
    // SecurityService.translationsInTask: neither user carries translations.edit, so the task is the only way in.
    userAccount = testData.withoutScope
    setTranslation().andIsForbidden

    userAccount = testData.withScope
    setTranslation().andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `commenting on a translation in the task needs the scope too`() {
    // SecurityService.checkScopeOrAssignedToTask: neither user carries translation-comments.add.
    userAccount = testData.withoutScope
    addComment().andIsForbidden

    userAccount = testData.withScope
    addComment().andIsCreated
  }

  private fun finishTask() = performProjectAuthPut("tasks/${TasksAssignedAccessTestData.TASK_NUMBER}/finish")

  private fun setTranslation() =
    performProjectAuthPut(
      "translations",
      mapOf(
        "key" to TasksAssignedAccessTestData.TASK_KEY_NAME,
        "translations" to mapOf("en" to "edited by the assignee"),
      ),
    )

  private fun addComment() =
    performProjectAuthPost(
      "translations/create-comment",
      mapOf(
        "keyId" to testData.taskKey.id,
        "languageId" to testData.englishLanguage.id,
        "text" to "comment by the assignee",
      ),
    )
}
