package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.TaskTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andIsForbidden
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
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * An OAuth token must not gain access through the user's task assignment: the assignee fallback in
 * SecurityService.checkTaskScopeOrAssigned would otherwise let a scoped-down token act past its consented scope.
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
  fun `an OAuth token cannot finish a task via assignment when its scope does not cover the operation`() {
    // projectUser is assigned to the translate task but has no TASKS_EDIT live permission (assignment is what normally
    // lets them finish it). A token scoped only to translations.view must NOT inherit that assignment-based widening.
    val token = mint(subject = testData.projectUser.self.id, scopes = listOf("translations.view"))

    mvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
          .put("/v2/projects/${testData.projectBuilder.self.id}/tasks/${testData.translateTask.self.number}/finish")
          .headers(bearer(token)),
      ).andIsForbidden
  }

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
