package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TasksAssignedAccessTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * The task-assignee elevation is gated on `Scope.TASKS_ASSIGNED_ACCESS`. These tests are about the granular-permission
 * population: role presets grant the scope by expansion, hand-picked scope sets only carry it if it was granted, and a
 * project API key carries it only if its own scope list does — which no key minted before the scope existed can.
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

    finishTask().andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a granular permission without tasks assigned-access cannot edit a translation its task covers`() {
    userAccount = testData.withoutScope

    setTranslation().andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a granular permission carrying tasks assigned-access can edit a translation its task covers`() {
    userAccount = testData.withScope

    setTranslation().andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a granular permission without tasks assigned-access cannot comment on a translation its task covers`() {
    userAccount = testData.withoutScope

    addComment().andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `a granular permission carrying tasks assigned-access can comment on a translation its task covers`() {
    userAccount = testData.withScope

    addComment().andIsCreated
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `without tasks assigned-access, a review assignee cannot change a translation's state`() {
    userAccount = testData.withoutScope

    setState().andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `carrying tasks assigned-access, a review assignee can change a translation's state`() {
    userAccount = testData.withScope

    setState().andIsOk
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW, Scope.TASKS_VIEW])
  fun `a key without tasks assigned-access loses the elevation its owner's permission carries`() {
    // The upgrade shape: the account keeps the scope, every key minted before it existed does not.
    userAccount = testData.withScope

    setTranslation().andIsForbidden
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW, Scope.TASKS_VIEW, Scope.TASKS_ASSIGNED_ACCESS])
  fun `a key carrying tasks assigned-access keeps the assignee elevation`() {
    userAccount = testData.withScope

    setTranslation().andIsOk
  }

  @Test
  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW, Scope.TASKS_VIEW, Scope.TASKS_ASSIGNED_ACCESS])
  fun `a key cannot grant an elevation its owner's permission lacks`() {
    userAccount = testData.withoutScope

    setTranslation().andIsForbidden
  }

  private fun setState() = performProjectAuthPut("translations/${testData.taskTranslation.id}/set-state/REVIEWED")

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
