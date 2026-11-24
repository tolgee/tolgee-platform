package io.tolgee.mcp

import io.tolgee.AbstractSpringTest
import io.tolgee.api.mcp.McpFacade
import io.tolgee.api.v2.controllers.ProjectStatsFacade
import io.tolgee.api.v2.controllers.translation.TranslationFacade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.ObjectMapper

@SpringBootTest
class McpToolDependenciesTest : AbstractSpringTest() {
  private val allowedInfrastructure =
    setOf<Class<*>>(
      McpRequestContext::class.java,
      ObjectMapper::class.java,
      PlatformTransactionManager::class.java,
    )

  /**
   * Facades without a REST twin: they serve read-only MCP tools whose REST counterparts
   * keep their own bodies. Checks added to those controller bodies do NOT reach MCP, so
   * adding one here needs the same scrutiny as a new controller body check.
   */
  private val mcpOnlyFacades =
    setOf<Class<*>>(
      TranslationFacade::class.java,
      ProjectStatsFacade::class.java,
    )

  @Test
  fun `MCP tools depend only on facades and infrastructure`() {
    val violations =
      toolClasses().flatMap { tool ->
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

  @Test
  fun `facades injected by MCP tools are shared with a REST controller`() {
    val controllerParamTypes =
      applicationContext
        .getBeansWithAnnotation(RestController::class.java)
        .values
        .map { ClassUtils.getUserClass(it) }
        .flatMap { controller -> controller.constructors.flatMap { it.parameterTypes.toList() } }
        .toSet()
    val unshared =
      toolClasses()
        .flatMap {
          it.constructors
            .first()
            .parameterTypes
            .toList()
        }.distinct()
        .filter { McpFacade::class.java.isAssignableFrom(it) }
        .filterNot { it in controllerParamTypes }
        .filterNot { it in mcpOnlyFacades }
    assertThat(unshared)
      .withFailMessage(
        "These facades are injected by MCP tools but by no REST controller, so they do " +
          "not actually share an endpoint body with REST. Inject the facade in the twin " +
          "controller, or list it in mcpOnlyFacades with the KDoc rules in mind: %s",
        unshared.map { it.simpleName },
      ).isEmpty()
  }

  private fun toolClasses() =
    applicationContext
      .getBeansOfType(McpToolsProvider::class.java)
      .values
      .map { ClassUtils.getUserClass(it) }
}
