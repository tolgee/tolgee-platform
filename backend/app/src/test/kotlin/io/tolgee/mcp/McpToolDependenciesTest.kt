package io.tolgee.mcp

import io.tolgee.AbstractSpringTest
import io.tolgee.api.mcp.McpFacade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.util.ClassUtils
import tools.jackson.databind.ObjectMapper

/**
 * MCP tools must run endpoint logic through [McpFacade] implementations, never by
 * injecting services directly. A direct service call skips whatever checks live in
 * the twin controller's body (GHSA-j998-54vh-rwpg).
 */
@SpringBootTest
class McpToolDependenciesTest : AbstractSpringTest() {
  private val allowedInfrastructure =
    setOf<Class<*>>(
      McpRequestContext::class.java,
      ObjectMapper::class.java,
      PlatformTransactionManager::class.java,
    )

  /** Tools written before the facade pattern were listed here; it must stay empty. */
  private val notMigratedYet = setOf<String>()

  @Test
  fun `MCP tools depend only on facades and infrastructure`() {
    val violations =
      applicationContext
        .getBeansOfType(McpToolsProvider::class.java)
        .values
        .map { ClassUtils.getUserClass(it) }
        .filterNot { it.simpleName in notMigratedYet }
        .flatMap { tool ->
          tool.constructors
            .first()
            .parameterTypes
            .filterNot { McpFacade::class.java.isAssignableFrom(it) }
            .filterNot { param -> allowedInfrastructure.any { it.isAssignableFrom(param) } }
            .map { "${tool.simpleName} injects ${it.simpleName}" }
        }
    assertThat(violations)
      .withFailMessage(
        "MCP tools may inject only McpFacade implementations and infrastructure. " +
          "Move the endpoint logic into a facade shared with the REST controller " +
          "(see McpFacade). Direct service calls skip controller-body checks " +
          "(GHSA-j998-54vh-rwpg). Violations: %s",
        violations,
      ).isEmpty()
  }
}
