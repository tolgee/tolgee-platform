package io.tolgee.mcp

import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider
import io.modelcontextprotocol.spec.McpStreamableServerSession
import io.tolgee.AbstractMcpTest
import io.tolgee.fixtures.RedisRunner
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import java.util.concurrent.ConcurrentHashMap

@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [McpRedisSessionRecoveryTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class McpRedisSessionRecoveryTest : AbstractMcpTest() {
  companion object {
    val redisRunner = RedisRunner()

    @AfterAll
    @JvmStatic
    fun stopRedis() {
      redisRunner.stop()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
        TestPropertyValues
          .of("spring.data.redis.port=${RedisRunner.port}")
          .applyTo(configurableApplicationContext)
      }
    }
  }

  @Autowired
  lateinit var transportProvider: WebMvcStreamableServerTransportProvider

  @Autowired
  lateinit var redissonClient: RedissonClient

  lateinit var data: McpPatTestData

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(data.testData.root)
  }

  @Test
  fun `session is recovered from Redis after local eviction`() {
    // 1. Initialize MCP client — this creates a session and the filter persists it to Redis
    val client = createMcpClientWithPat(data.pat.token!!)

    // Verify the tool call works before eviction
    val resultBefore = callTool(client, "list_projects")
    assertThat(resultBefore.isError).isFalse()

    // 2. Get the sessions map via reflection and find the session ID
    val sessionsMap = getSessionsMap()
    assertThat(sessionsMap).isNotEmpty

    val sessionId = sessionsMap.keys().toList().first()

    // Verify session data is in Redis
    val bucket = redissonClient.getBucket<String>("mcp_session:$sessionId")
    assertThat(bucket.get()).isNotNull

    // 3. Remove session from local map to simulate request landing on a different replica
    sessionsMap.remove(sessionId)
    assertThat(sessionsMap.containsKey(sessionId)).isFalse()

    // 4. Call a tool — the filter should recover the session from Redis
    val resultAfter = callTool(client, "list_projects")
    assertThat(resultAfter.isError).isFalse()

    // 5. Verify session is back in local map
    assertThat(sessionsMap.containsKey(sessionId)).isTrue()
  }

  @Suppress("UNCHECKED_CAST")
  private fun getSessionsMap(): ConcurrentHashMap<String, McpStreamableServerSession> {
    val field = WebMvcStreamableServerTransportProvider::class.java.getDeclaredField("sessions")
    field.isAccessible = true
    return field.get(transportProvider) as ConcurrentHashMap<String, McpStreamableServerSession>
  }
}
