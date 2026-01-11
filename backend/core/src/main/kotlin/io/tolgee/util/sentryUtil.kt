package io.tolgee.util

import io.sentry.Sentry

inline fun <T> runSentryCatching(fn: () -> T): T {
  try {
    return fn()
  } catch (e: Exception) {
    Sentry.captureException(e)
    throw e
  }
}
