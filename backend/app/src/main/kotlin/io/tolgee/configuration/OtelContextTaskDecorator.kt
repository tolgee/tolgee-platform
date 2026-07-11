package io.tolgee.configuration

import io.opentelemetry.context.Context
import org.springframework.core.task.TaskDecorator

/**
 * TaskDecorator that propagates OpenTelemetry context to async tasks.
 *
 * This ensures that spans created in @Async methods and scheduled tasks
 * are properly linked to their parent spans, maintaining trace continuity.
 */
class OtelContextTaskDecorator : TaskDecorator {
  override fun decorate(runnable: Runnable): Runnable {
    // Capture current OTEL context
    val context = Context.current()

    return Runnable {
      // Restore context in the new thread
      context.makeCurrent().use {
        runnable.run()
      }
    }
  }
}
