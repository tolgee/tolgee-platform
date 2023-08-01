package io.tolgee.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Logging

val <T : Logging> T.logger: Logger get() = LoggerFactory.getLogger(javaClass)

fun Logger.debug(message: () -> String) {
  if (this.isDebugEnabled) {
    this.debug(message())
  }
}
