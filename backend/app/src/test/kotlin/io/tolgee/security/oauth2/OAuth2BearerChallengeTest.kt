package io.tolgee.security.oauth2

import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.enums.Scope
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

/**
 * What a refused request tells the caller: the RFC 6750 `WWW-Authenticate` challenge, and the RFC 9728 metadata
 * pointer that only the MCP resource advertises. The grant flow itself lives in [OAuth2AuthorizationCodeFlowTest].
 */
class OAuth2BearerChallengeTest : AbstractControllerTest() {
  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an unauthorized API request carries an RFC 6750 bearer challenge`() {
    val challenge = apiRequestChallenge("$OAUTH_ACCESS_TOKEN_PREFIX-not-a-token")

    challenge.assert
      .isNotNull()
      .contains("Bearer")
      .contains("""error="invalid_token"""")
  }

  @Test
  fun `an unauthorized MCP request points at the RFC 9728 metadata document`() {
    val challenge =
      mvc
        .perform(get("/mcp/developer").header("Authorization", "Bearer ${OAUTH_ACCESS_TOKEN_PREFIX}nope"))
        .andReturn()
        .response
        .getHeader("WWW-Authenticate")

    challenge.assert.isNotNull().contains("resource_metadata=")
    challenge.assert.contains("/.well-known/oauth-protected-resource/mcp/developer")
  }

  @Test
  fun `a refusal outside the MCP resource carries the challenge but no metadata pointer`() {
    val challenge = apiRequestChallenge("${OAUTH_ACCESS_TOKEN_PREFIX}nope")

    challenge.assert.isNotNull().contains("invalid_token")
    challenge!!.assert.doesNotContain("resource_metadata")
  }

  @Test
  fun `a scope refusal for an X-API-Key caller carries no Bearer challenge`() {
    // RFC 6750 §3.1 scopes insufficient_scope to callers that presented a bearer token; a PAK caller did not.
    val narrowPak =
      "tgpak_" + apiKeyService.create(testData.user, setOf(Scope.TRANSLATIONS_VIEW), testData.project).encodedKey
    val response =
      mvc
        .perform(
          post("/v2/projects/${testData.project.id}/keys")
            .header("X-API-Key", narrowPak)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name":"whatever"}"""),
        ).andReturn()
        .response

    response.status.assert.isEqualTo(403)
    response.getHeader("WWW-Authenticate").assert.isNull()
  }

  @Test
  fun `a 401 outside the MCP resource is not an OAuth challenge at all`() {
    val challenge =
      mvc
        .perform(get("/v2/projects/${testData.project.id}/translations").header("X-API-Key", "tgpak_not-a-real-key"))
        .andReturn()
        .response
        .getHeader("WWW-Authenticate")

    challenge.assert.isNull()
  }

  @Test
  fun `the webapp's own JWT is not an OAuth caller, so its refusals carry no challenge`() {
    // The webapp authenticates with `Authorization: Bearer <JWT>` too; only the OAuth token prefix makes a caller one.
    val response =
      mvc
        .perform(get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer not-a-jwt"))
        .andReturn()
        .response

    response.status.assert.isEqualTo(401)
    response.getHeader("WWW-Authenticate").assert.isNull()
  }

  private fun apiRequestChallenge(accessToken: String): String? =
    mvc
      .perform(
        get("/v2/projects/${testData.project.id}/translations").header("Authorization", "Bearer $accessToken"),
      ).andReturn()
      .response
      .getHeader("WWW-Authenticate")
}
