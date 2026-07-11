package io.tolgee.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

interface Logging {
  companion object {
    /**
     * Storage for time sums. Used for counting sum times.
     */
    val timeSums = mutableMapOf<Pair<KClass<*>, String>, Duration>()
  }

  fun <T> traceLogMeasureTime(
    operationName: String,
    fn: () -> T,
  ): T {
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

inline fun <reified T> T.logger(): Logger {
  return LoggerFactory.getLogger(T::class.java)
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

inline fun <reified T> Logger.traceMeasureTime(
  message: String,
  block: () -> T,
): T {
  if (this.isTraceEnabled) {
    return measureTime(message, this::trace, block)
  }
  return block()
}

inline fun <reified T> Logger.infoMeasureTime(
  message: String,
  block: () -> T,
): T {
  return measureTime(message, this::info, block)
}

inline fun <reified T> Logger.measureTime(
  message: String,
  printFn: (String) -> Unit,
  block: () -> T,
): T {
  val (result, duration) = measureTimedValue(block)
  printFn("$message: $duration")
  return result
}

inline fun <reified T> Logger.storeTraceTimeSum(
  id: String,
  block: () -> T,
): T {
  return if (this.isTraceEnabled) {
    val (result, duration) = measureTimedValue(block)
    Logging.timeSums.compute(this::class to id) { _, v ->
      (v ?: Duration.ZERO) + duration
    }
    result
  } else {
    block()
  }
}

fun Logger.traceLogTimeSum(
  id: String,
  unit: DurationUnit = DurationUnit.MILLISECONDS,
) {
  if (this.isTraceEnabled) {
    val duration = Logging.timeSums[this::class to id]
    if (duration != null) {
      this.trace("SUM $id: ${duration.toString(unit)}")
    }
  }
}
