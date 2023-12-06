package io.tolgee.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Logging {
  fun <T> traceLogMeasureTime(operationName: String, fn: () -> T): T {
    if (logger.isTraceEnabled) {
      val start = System.currentTimeMillis()
      logger.trace("Operation $operationName started")
      val result = fn()
      val end = System.currentTimeMillis()
      logger.trace("Execution time $operationName: ${end - start}ms")
      return result
    }
    return fn()
  }
}

val <T : Logging> T.logger: Logger get() = LoggerFactory.getLogger(javaClass)

fun Logger.trace(message: () -> String) {
  if (this.isTraceEnabled) {
    this.trace(message())
  }
}

fun Logger.debug(message: () -> String) {
  if (this.isDebugEnabled) {
    this.debug(message())
  }
}
