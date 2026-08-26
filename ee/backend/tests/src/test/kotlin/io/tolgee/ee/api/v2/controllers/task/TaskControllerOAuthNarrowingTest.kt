package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2AudienceResolver
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The task-assignee elevation is gated on [Scope.TASKS_ASSIGNED_ACCESS]. A credential that does not carry that scope
 * cannot ride the assignment past its own authority, whatever the underlying user is assigned to.
 */
class TaskControllerOAuthNarrowingTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtEncoder: JwtEncoder

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: TaskTestData

  @BeforeEach
  fun setup() {
    testData = TaskTestData()
    testDataService.saveTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
    // Tokens are minted directly (bypassing SAS); insert an authorization row the resolver's liveness check can find.
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", LIVE_AUTHORIZATION_ID)
    jdbcTemplate.update(
      "INSERT INTO oauth2_authorization (id, registered_client_id, principal_name, authorization_grant_type) " +
        "VALUES (?, ?, ?, ?)",
      LIVE_AUTHORIZATION_ID,
      "test-client",
      "test",
      "authorization_code",
    )
  }

  @AfterEach
  fun cleanup() {
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", LIVE_AUTHORIZATION_ID)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an OAuth token without tasks assigned-access cannot ride the assignment`() {
    // projectUser IS assigned to the translate task, but the token's scope set omits TASKS_ASSIGNED_ACCESS, so the
    // elevation must not apply and the request is permission-denied rather than reaching the business rule.
    val token = mint(subject = testData.projectUser.self.id, scopes = listOf("translations.view"))

    finishTranslateTask(token).andIsForbidden
  }

  @Test
  fun `an OAuth token with tasks assigned-access reaches a task via assignment`() {
    // Same assignment, but the scope is consented. The request is NOT permission-denied — it passes the permission gate
    // and is rejected only by the business rule that the task isn't finished, proving the elevation is applied.
    val token =
      mint(
        subject = testData.projectUser.self.id,
        scopes = listOf("translations.view", Scope.TASKS_ASSIGNED_ACCESS.value),
      )

    finishTranslateTask(token)
      .andIsBadRequest
      .andHasErrorMessage(Message.TASK_NOT_FINISHED)
  }

  private fun finishTranslateTask(token: String) =
    mvc.perform(
      MockMvcRequestBuilders
        .put("/v2/projects/${testData.projectBuilder.self.id}/tasks/${testData.translateTask.self.number}/finish")
        .headers(bearer(token)),
    )

  private fun bearer(token: String) = HttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, "Bearer $token") }

  private fun mint(
    subject: Long,
    scopes: List<String>,
  ): String {
    val now = Instant.now()
    val claims =
      JwtClaimsSet
        .builder()
        .subject(subject.toString())
        .audience(listOf(apiAudience))
        .issuedAt(now)
        .expiresAt(now.plus(30, ChronoUnit.MINUTES))
        .claim("scope", scopes)
        .claim(OAuth2Constants.PROJECTS_CLAIM, OAuth2Constants.ALL_PROJECTS)
        .claim(OAuth2Constants.AUTHORIZATION_ID_CLAIM, LIVE_AUTHORIZATION_ID)
        .build()
    val header = JwsHeader.with(SignatureAlgorithm.RS256).build()
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
  }

  private val apiAudience: String
    get() =
      tolgeeProperties.backEndUrl
        ?: tolgeeProperties.frontEndUrl
        ?: OAuth2AudienceResolver.DEFAULT_API_AUDIENCE

  companion object {
    private const val LIVE_AUTHORIZATION_ID = "task-oauth-narrowing-test-authz"
  }
}
