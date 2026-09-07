package io.tolgee.configuration

/**
 * The executor itself is defined in the app module, next to the other one, because the task
 * decorators it has to share live there. This only names it.
 */
object SessionBookkeepingExecutor {
  const val BEAN_NAME = "sessionBookkeepingExecutor"
}
