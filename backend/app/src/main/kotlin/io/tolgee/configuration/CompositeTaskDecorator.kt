package io.tolgee.configuration

import org.springframework.core.task.TaskDecorator

/**
 * Composes multiple TaskDecorators into a single decorator.
 * Decorators are applied in order (first decorator wraps the result of subsequent decorators).
 */
class CompositeTaskDecorator(
  private vararg val decorators: TaskDecorator,
) : TaskDecorator {
  override fun decorate(runnable: Runnable): Runnable {
    return decorators.foldRight(runnable) { decorator, acc ->
      decorator.decorate(acc)
    }
  }
}
