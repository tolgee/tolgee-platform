package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.OAuth2TestTokens
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.model.enums.Scope
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

/**
 * The task-assignee elevation is gated on [Scope.TASKS_ASSIGNED_ACCESS]. A credential that does not carry that scope
 * cannot ride the assignment past its own authority, whatever the underlying user is assigned to.
 */
class TaskControllerOAuthNarrowingTest : AbstractControllerTest() {
  @Autowired
  private lateinit var authorizationRepository: OAuth2AuthorizationRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private lateinit var testData: TaskTestData
  private lateinit var tokens: OAuth2TestTokens

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    testDataService.saveTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
    tokens = OAuth2TestTokens(authorizationRepository, userAccountService, keyGenerator)
  }

  @AfterEach
  fun cleanup() {
    tokens.deleteAll()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an OAuth token without tasks assigned-access cannot ride the assignment`() {
    // projectUser IS assigned to the translate task, but the token's scope set omits TASKS_ASSIGNED_ACCESS, so the
    // elevation must not apply and the request is permission-denied rather than reaching the business rule.
    val token = issue(listOf(Scope.TRANSLATIONS_VIEW.value))

    finishTranslateTask(token).andIsForbidden
  }

  @Test
  fun `an OAuth token with tasks assigned-access reaches a task via assignment`() {
    // Same assignment, but the scope is consented. The request is NOT permission-denied — it passes the permission gate
    // and is rejected only by the business rule that the task isn't finished, proving the elevation is applied.
    val token = issue(listOf(Scope.TRANSLATIONS_VIEW.value, Scope.TASKS_ASSIGNED_ACCESS.value))

    finishTranslateTask(token)
      .andIsBadRequest
      .andHasErrorMessage(Message.TASK_NOT_FINISHED)
  }

  private fun issue(scopes: List<String>) = tokens.issue(subject = testData.projectUser.self.id, scopes = scopes)

  private fun finishTranslateTask(token: String) =
    mvc.perform(
      MockMvcRequestBuilders
        .put("/v2/projects/${testData.projectBuilder.self.id}/tasks/${testData.translateTask.self.number}/finish")
        .headers(bearer(token)),
    )

  private fun bearer(token: String) = HttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, "Bearer $token") }
}
