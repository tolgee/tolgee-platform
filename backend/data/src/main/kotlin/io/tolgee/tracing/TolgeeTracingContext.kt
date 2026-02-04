package io.tolgee.tracing

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext as kotlinWithContext

/**
 * Utility for setting project/org context in OpenTelemetry traces
 * and propagating OTEL context across coroutine boundaries.
 *
 * This component works for ALL entry points:
 * - HTTP requests (called from interceptors)
 * - Batch jobs (called from BatchJobActionService)
 * - Any other async boundary
 */
@Component
class TolgeeTracingContext {
  /**
   * Adds project and organization context to the current span as attributes.
   * Call this from any entry point (HTTP interceptor, batch job handler).
   *
   * Note: Attributes are set on the current span only. Child spans created
   * by OTEL auto-instrumentation will be linked via trace ID but won't
   * automatically inherit these attributes.
   */
  fun setContext(
    projectId: Long?,
    organizationId: Long?,
  ) {
    val span = Span.current()

    // Add as span attributes (visible in this span)
    projectId?.let { span.setAttribute("tolgee.project.id", it) }
    organizationId?.let { span.setAttribute("tolgee.organization.id", it) }
  }

  /**
   * Returns a CoroutineContext element that propagates the current OTEL context
   * across coroutine suspension/resumption. Use when launching coroutines:
   *
   * ```kotlin
   * launch(tracingContext.asCoroutineContext()) {
   *   // OTEL context is preserved across suspension points
   * }
   * ```
   */
  fun asCoroutineContext(): CoroutineContext {
    return Context.current().asContextElement()
  }

  /**
   * Execute a suspend block with OTEL context properly propagated.
   * Use for nested coroutine operations when context might not be set.
   *
   * ```kotlin
   * tracingContext.withPropagatedContext {
   *   // nested async work with preserved context
   * }
   * ```
   */
  suspend fun <T> withPropagatedContext(block: suspend CoroutineScope.() -> T): T {
    return kotlinWithContext(Context.current().asContextElement()) {
      block()
    }
  }
}
