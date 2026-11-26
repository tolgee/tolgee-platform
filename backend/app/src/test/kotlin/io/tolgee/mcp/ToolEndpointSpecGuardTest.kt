package io.tolgee.mcp

import io.tolgee.security.McpGuardTestAnnotation
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authorization.IsGlobalRoute
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ToolEndpointSpecGuardTest {
  @Test
  fun `buildSpec fails on a security annotation the MCP mirror does not handle`() {
    assertThatThrownBy { buildSpec(Fixture::withUnhandledAnnotation, "test_op") }
      .hasMessageContaining("BypassEmailVerification")
      .hasMessageContaining("McpRequestContext")
  }

  @Test
  fun `buildSpec accepts a method with only handled annotations`() {
    assertThatCode { buildSpec(Fixture::withHandledAnnotations, "test_op") }
      .doesNotThrowAnyException()
  }

  @Test
  fun `buildSpec fails on an unhandled annotation carried by a composed annotation`() {
    assertThatThrownBy { buildSpec(Fixture::withComposedUnhandledAnnotation, "test_op") }
      .hasMessageContaining("McpGuardTestAnnotation")
      .hasMessageContaining("McpRequestContext")
  }

  @Retention(AnnotationRetention.RUNTIME)
  @Target(AnnotationTarget.FUNCTION)
  @McpGuardTestAnnotation
  private annotation class ComposedWithUnhandled

  @Suppress("unused")
  private class Fixture {
    @AllowApiAccess
    @IsGlobalRoute
    @BypassEmailVerification
    fun withUnhandledAnnotation() {}

    @AllowApiAccess
    @IsGlobalRoute
    fun withHandledAnnotations() {}

    @AllowApiAccess
    @IsGlobalRoute
    @ComposedWithUnhandled
    fun withComposedUnhandledAnnotation() {}
  }
}
