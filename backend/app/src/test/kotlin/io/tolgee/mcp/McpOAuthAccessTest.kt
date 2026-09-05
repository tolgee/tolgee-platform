package io.tolgee.mcp

import io.tolgee.AbstractMcpTest
import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.McpOAuthTestData
import io.tolgee.fixtures.OAuth2TestTokens
import io.tolgee.model.enums.Scope
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * The MCP endpoint is the reason this authorization server exists, and it is a RouterFunction: the servlet
 * interceptors never run for it, so its OAuth enforcement is a parallel implementation in [McpRequestContext].
 * These cases drive a real bearer token over HTTP through that path rather than mocking the facade.
 */
class McpOAuthAccessTest : AbstractMcpTest() {
  @Autowired
  private lateinit var grantRepository: OAuth2GrantRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private lateinit var tokens: OAuth2TestTokens
  private lateinit var testData: McpOAuthTestData

  @BeforeEach
  fun setup() {
    tokens = OAuth2TestTokens(grantRepository, userAccountService, keyGenerator)
    testData = McpOAuthTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    tokens.deleteAll()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a project-scoped tool succeeds for a token holding the project and the scope`() {
    val token = tokenFor(listOf(Scope.KEYS_VIEW.value, Scope.TRANSLATIONS_VIEW.value), testData.project.id)

    val result = callToolAndGetJson(createMcpClientWithBearer(token), "list_keys", projectArgument())

    assertThat(result["items"]).isNotNull
  }

  @Test
  fun `a project-scoped tool is refused for a token bound to another project`() {
    val token = tokenFor(listOf(Scope.KEYS_VIEW.value, Scope.TRANSLATIONS_VIEW.value), testData.otherProject.id)

    assertToolFails(
      createMcpClientWithBearer(token),
      "list_keys",
      projectArgument(),
      expectedError = "user_has_no_project_access",
    )
  }

  @Test
  fun `a project-scoped tool is refused for a token that does not carry the scope`() {
    // activity.view is standalone in the scope hierarchy; translations.view would expand to keys.view and pass.
    val token = tokenFor(listOf(Scope.ACTIVITY_VIEW.value), testData.project.id)

    assertToolFails(createMcpClientWithBearer(token), "list_keys", projectArgument())
  }

  @Test
  fun `a tool outside the project-scoped surface is refused for any OAuth token`() {
    val token = tokenFor(Scope.entries.map { it.value }, testData.project.id)

    assertToolFails(
      createMcpClientWithBearer(token),
      "list_projects",
      expectedError = "oauth_access_not_allowed",
    )
  }

  @Test
  fun `a revoked token no longer authenticates`() {
    val token = tokenFor(listOf(Scope.KEYS_VIEW.value, Scope.TRANSLATIONS_VIEW.value), testData.project.id)
    tokens.revoke(token)

    val response = mcpInitializeWith(token)

    response.statusCode().assert.isEqualTo(401)
    response
      .headers()
      .firstValue("WWW-Authenticate")
      .orElse("")
      .assert
      .contains("invalid_token")
  }

  @Test
  fun `a token bound to one project needs no explicit projectId`() {
    val token = tokenFor(listOf(Scope.KEYS_VIEW.value, Scope.TRANSLATIONS_VIEW.value), testData.project.id)

    val result = callToolAndGetJson(createMcpClientWithBearer(token), "list_keys")

    assertThat(result["items"]).isNotNull
  }

  @Test
  fun `an all-projects token has no implicit project to fall back on`() {
    val token = tokens.issue(subject = testData.user.id, scopes = listOf(Scope.KEYS_VIEW.value), projectIds = null)

    assertToolFails(createMcpClientWithBearer(token), "list_keys", expectedError = "project_not_selected")
  }

  private fun projectArgument() = mapOf("projectId" to testData.project.id)

  private fun mcpInitializeWith(token: String): HttpResponse<String> {
    val body =
      """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",""" +
        """"capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}}}"""
    val request =
      HttpRequest
        .newBuilder(URI.create("http://localhost:$port/mcp/developer"))
        .header("Authorization", "Bearer $token")
        .header("Content-Type", "application/json")
        .header("Accept", "application/json, text/event-stream")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
  }

  private fun tokenFor(
    scopes: List<String>,
    projectId: Long,
  ): String =
    tokens.issue(
      subject = testData.user.id,
      scopes = scopes,
      projectIds = listOf(projectId),
    )
}
