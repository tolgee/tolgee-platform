package io.tolgee.mcp

import com.posthog.server.PostHog
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.AbstractMcpTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.configuration.tolgee.RateLimitProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.assertPostHogEventReported
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.security.ratelimit.RateLimitedException
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Duration
import java.util.Date

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = ["tolgee.cache.enabled=true"],
)
class McpServerIntegrationTest : AbstractMcpTest() {
  @MockitoBean
  @Autowired
  lateinit var postHog: PostHog

  @Autowired
  lateinit var activityRevisionRepository: ActivityRevisionRepository

  @MockitoSpyBean
  @Autowired
  lateinit var authenticationFacadeSpy: AuthenticationFacade

  @Autowired
  lateinit var mcpRequestContext: McpRequestContext

  @Autowired
  lateinit var rateLimitProperties: RateLimitProperties

  @Autowired
  lateinit var rateLimitService: io.tolgee.security.ratelimit.RateLimitService

  lateinit var data: McpTestData

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    Mockito.reset(postHog)
    // reset() on a spy clears all stubbings, reverting to real method delegation
    Mockito.reset(authenticationFacadeSpy)
  }

  @Test
  fun `write tool records activity in database`() {
    val client = createMcpClient(data.pat.token!!)

    callTool(
      client,
      "create_language",
      mapOf("projectId" to data.projectId, "name" to "German", "tag" to "de"),
    )

    waitForNotThrowing(timeout = 5000) {
      val revisions =
        activityRevisionRepository.getForProject(
          data.projectId,
          PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp")),
          listOf(ActivityType.CREATE_LANGUAGE),
        )
      assertThat(revisions.content).isNotEmpty
      assertThat(revisions.content.first().type).isEqualTo(ActivityType.CREATE_LANGUAGE)
    }
  }

  @Test
  fun `tool call emits PostHog event with mcp metadata`() {
    val client = createMcpClient(data.pat.token!!)

    callTool(
      client,
      "create_language",
      mapOf("projectId" to data.projectId, "name" to "French", "tag" to "fr"),
    )

    val eventData = assertPostHogEventReported(postHog, "CREATE_LANGUAGE")
    assertThat(eventData["mcp"]).isEqualTo("true")
    assertThat(eventData["mcp_operation"]).isEqualTo("create_language")
  }

  @Test
  fun `tool call with invalid PAT returns error`() {
    val transport =
      HttpClientStreamableHttpTransport
        .builder("http://localhost:$port")
        .endpoint("/mcp/developer")
        .customizeRequest { builder ->
          builder.header("X-API-Key", "tgpat_invalid_token_here")
        }.build()

    val client =
      McpClient
        .sync(transport)
        .clientInfo(McpSchema.Implementation("test-client", "1.0"))
        .requestTimeout(Duration.ofSeconds(10))
        .build()

    try {
      client.initialize()
      val result = callTool(client, "list_projects")
      assertThat(result.isError).isTrue()
    } catch (e: Exception) {
      assertThat(e).isNotNull()
    } finally {
      try {
        client.close()
      } catch (_: Exception) {
      }
    }
  }

  @Test
  fun `tool call with expired PAT returns error`() {
    var expiredPat: io.tolgee.model.Pat? = null
    testDataService.saveTestData {
      addUserAccount {
        username = "expired_pat_mcp_user"
      }.build {
        addPat {
          description = "Expired PAT for MCP"
          expiresAt = Date(System.currentTimeMillis() - 100000)
          expiredPat = this
        }
      }
    }

    val transport =
      HttpClientStreamableHttpTransport
        .builder("http://localhost:$port")
        .endpoint("/mcp/developer")
        .customizeRequest { builder ->
          builder.header("X-API-Key", "tgpat_${expiredPat!!.token}")
        }.build()

    val client =
      McpClient
        .sync(transport)
        .clientInfo(McpSchema.Implementation("test-client", "1.0"))
        .requestTimeout(Duration.ofSeconds(10))
        .build()

    try {
      client.initialize()
      val result = callTool(client, "list_projects")
      assertThat(result.isError).isTrue()
    } catch (e: Exception) {
      assertThat(e).isNotNull()
    } finally {
      try {
        client.close()
      } catch (_: Exception) {
      }
    }
  }

  @Test
  fun `read-only mode allows read tools`() {
    doReturn(true).whenever(authenticationFacadeSpy).isReadOnly

    val client = createMcpClient(data.pat.token!!)

    val readResult = callTool(client, "list_projects")
    assertThat(readResult.isError).isFalse()
  }

  @Test
  fun `read-only mode blocks write tools`() {
    doReturn(true).whenever(authenticationFacadeSpy).isReadOnly

    val client = createMcpClient(data.pat.token!!)

    // MCP SDK throws McpError for server-side exceptions rather than returning isError
    assertThat(
      runCatching {
        callTool(
          client,
          "create_keys",
          mapOf(
            "projectId" to data.projectId,
            "keys" to listOf(mapOf("name" to "readonly.test")),
          ),
        )
      }.exceptionOrNull(),
    ).isNotNull()
      .hasMessageContaining("read_only_mode")
  }

  @Test
  fun `rapid tool calls all succeed without false rate limiting`() {
    val client = createMcpClient(data.pat.token!!)

    val results =
      (1..10).map {
        callTool(client, "list_projects")
      }

    results.forEach { result ->
      assertThat(result.isError).isFalse()
    }
  }

  @Test
  fun `applyRateLimit throws RateLimitedException when bucket is exhausted`() {
    // Ensure rate limiting is enabled for this test
    @Suppress("DEPRECATION")
    val prevEnabled = rateLimitProperties.enabled
    val prevEndpointLimits = rateLimitProperties.endpointLimits

    @Suppress("DEPRECATION")
    rateLimitProperties.enabled = true
    rateLimitProperties.endpointLimits = true

    // Set up authentication context so RateLimitService can identify the user
    val auth =
      TolgeeAuthentication(
        credentials = null,
        deviceId = null,
        userAccount = UserAccountDto.fromEntity(data.testData.user),
        actingAsUserAccount = null,
        isReadOnly = false,
      )
    SecurityContextHolder.getContext().authentication = auth

    try {
      // Verify preconditions: rate limiting is enabled and user is authenticated
      assertThat(rateLimitService.shouldRateLimit(false)).isTrue()
      assertThat(authenticationFacadeSpy.authenticatedUserOrNull).isNotNull()

      // Create a spec with a tight rate limit (2 calls per minute)
      val rateLimitedSpec =
        ToolEndpointSpec(
          mcpOperation = "test_rate_limited",
          requiredScopes = null,
          allowedTokenType = AuthTokenType.ANY,
          isWriteOperation = false,
          isGlobalRoute = true,
          rateLimitPolicy = RateLimitSpec(limit = 2, refillDurationInMs = 60_000),
        )

      // First 2 calls should succeed (bucket has 2 tokens)
      repeat(2) {
        transactionTemplate.execute {
          mcpRequestContext.executeAs(rateLimitedSpec) { "ok" }
        }
      }

      // 3rd call should exhaust the bucket and throw RateLimitedException
      assertThat(
        runCatching {
          transactionTemplate.execute {
            mcpRequestContext.executeAs(rateLimitedSpec) { "ok" }
          }
        }.exceptionOrNull(),
      ).isInstanceOf(RateLimitedException::class.java)
    } finally {
      SecurityContextHolder.clearContext()

      @Suppress("DEPRECATION")
      rateLimitProperties.enabled = prevEnabled
      rateLimitProperties.endpointLimits = prevEndpointLimits

      // Evict rate limit cache entry to avoid leaking state to other tests
      cacheManager.getCache(io.tolgee.constants.Caches.RATE_LIMITS)?.clear()
    }
  }
}
