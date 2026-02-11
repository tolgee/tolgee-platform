package io.tolgee.mcp.tools.spec

import io.tolgee.model.enums.Scope
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class ProjectContextSetupTest : McpSecurityTestBase() {
  @Test
  fun `isGlobalRoute true does not call projectContextService setup`() {
    sut.executeAs(spec(isGlobalRoute = true), projectId = 1L) {}

    verify(projectContextService, never()).setup(any<Long>(), any(), any(), any())
  }

  @Test
  fun `projectId null does not call projectContextService setup`() {
    sut.executeAs(spec(isGlobalRoute = false), projectId = null) {}

    verify(projectContextService, never()).setup(any<Long>(), any(), any(), any())
  }

  @Test
  fun `isGlobalRoute false with projectId calls setup with correct args`() {
    val scopes = arrayOf(Scope.KEYS_VIEW)

    sut.executeAs(
      spec(isGlobalRoute = false, requiredScopes = scopes, useDefaultPermissions = true, isWriteOperation = true),
      projectId = 99L,
    ) {}

    verify(projectContextService).setup(
      eq(99L),
      eq(scopes),
      eq(true),
      eq(true),
    )
  }
}
