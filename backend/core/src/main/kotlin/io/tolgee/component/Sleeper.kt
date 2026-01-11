package io.tolgee.component

import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component

@Component
class Sleeper : Logging {
  fun sleep(timeInMs: Long) {
    val duration = getDuration(timeInMs)
    logger.debug("Sleeping for $duration.")
    Thread.sleep(duration)
    logger.debug("Sleeping done.")
  }

  /**
   * This helps us to mock a sleep time in tests
   */
  fun getDuration(timeInMs: Long): Long {
    return timeInMs
  }
}
