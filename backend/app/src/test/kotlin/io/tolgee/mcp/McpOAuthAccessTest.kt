package io.tolgee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.McpOAuthTestData
import io.tolgee.fixtures.OAuth2TestTokens
import io.tolgee.model.enums.Scope
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.OAuth2Audience
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.JsonNode
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
  private lateinit var oauth2GrantRepository: OAuth2GrantRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private lateinit var oauth2Tokens: OAuth2TestTokens
  private lateinit var testData: McpOAuthTestData

  @BeforeEach
  fun setup() {
    oauth2Tokens = OAuth2TestTokens(oauth2GrantRepository, userAccountService, keyGenerator)
    testData = McpOAuthTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    oauth2Tokens.deleteAll()
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
  fun `an OAuth client is not offered create_project`() {
    val token = tokenFor(Scope.entries.map { it.value }, null)

    val toolNames = listedToolNames(createMcpClientWithBearer(token))

    assertThat(toolNames).contains("list_projects")
    assertThat(toolNames).doesNotContain("create_project")
  }

  @Test
  fun `create_project cannot be called with an OAuth token`() {
    val token = tokenFor(Scope.entries.map { it.value }, null)

    assertToolFails(
      createMcpClientWithBearer(token),
      "create_project",
      mapOf(
        "name" to "OAuth project",
        "organizationId" to testData.project.organizationOwner.id,
        "languages" to listOf(mapOf("name" to "English", "tag" to "en")),
      ),
      expectedError = "create_project",
    )
  }

  @Test
  fun `a client that has not logged in is not offered create_project`() {
    assertThat(listedToolNames(createMcpClientWithoutAuth())).doesNotContain("create_project")
  }

  @Test
  fun `a personal access token client is offered create_project`() {
    val patData = createTestDataWithPat()
    try {
      assertThat(listedToolNames(createMcpClientWithPat(patData.pat.token!!))).contains("create_project")
    } finally {
      testDataService.cleanTestData(patData.testData.root)
    }
  }

  @Test
  fun `list_projects returns only the project a token is bound to`() {
    val token = tokenFor(listOf(Scope.KEYS_VIEW.value), testData.project.id)

    val result = callToolAndGetJson(createMcpClientWithBearer(token), "list_projects")

    assertThat(listedProjectIds(result)).isEqualTo(listOf(testData.project.id))
  }

  @Test
  fun `list_projects returns every project the user can reach for an all-projects token`() {
    val token =
      oauth2Tokens.issue(
        subject = testData.user.id,
        scopes = listOf(Scope.KEYS_VIEW.value),
        projectIds = null,
        audience = OAuth2Audience.MCP,
      )

    val result = callToolAndGetJson(createMcpClientWithBearer(token), "list_projects")

    assertThat(listedProjectIds(result).containsAll(listOf(testData.project.id, testData.otherProject.id))).isTrue()
  }

  @Test
  fun `a revoked token no longer authenticates`() {
    val token = tokenFor(listOf(Scope.KEYS_VIEW.value, Scope.TRANSLATIONS_VIEW.value), testData.project.id)
    oauth2Tokens.revoke(token)

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
    val token =
      oauth2Tokens.issue(
        subject = testData.user.id,
        scopes = listOf(Scope.KEYS_VIEW.value),
        projectIds = null,
        audience = OAuth2Audience.MCP,
      )

    assertToolFails(createMcpClientWithBearer(token), "list_keys", expectedError = "project_not_selected")
  }

  @Test
  fun `an API-audience token is refused by the MCP endpoint with invalid_token`() {
    val token =
      oauth2Tokens.issue(
        subject = testData.user.id,
        scopes = listOf(Scope.KEYS_VIEW.value, Scope.TRANSLATIONS_VIEW.value),
        projectIds = listOf(testData.project.id),
        audience = OAuth2Audience.API,
      )

    val response = mcpInitializeWith(token)

    response.statusCode().assert.isEqualTo(401)
    response
      .headers()
      .firstValue("WWW-Authenticate")
      .orElse("")
      .assert
      .contains("invalid_token")
  }

  private fun listedProjectIds(result: JsonNode): List<Long> {
    val items = result["items"]
    return (0 until items.size()).map { items[it]["id"].asLong() }
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

  private fun listedToolNames(client: McpSyncClient): List<String> = client.listTools().tools().map { it.name() }

  private fun tokenFor(
    scopes: List<String>,
    projectId: Long?,
  ): String =
    oauth2Tokens.issue(
      subject = testData.user.id,
      scopes = scopes,
      projectIds = projectId?.let { listOf(it) },
      audience = OAuth2Audience.MCP,
    )
}
