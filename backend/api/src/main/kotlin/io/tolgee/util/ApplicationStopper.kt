package io.tolgee.util

import io.tolgee.configuration.tolgee.InternalProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class ApplicationStopper(
  val internalProperties: InternalProperties,
  val applicationContext: ApplicationContext,
) {
  private val log = LoggerFactory.getLogger(ApplicationStopper::class.java)

  @EventListener(ApplicationReadyEvent::class)
  fun handleApplicationReady() {
    if (internalProperties.stopRightAfterStart) {
      log.info("Exiting: StopRightAfterStart property is set to true")
      SpringApplication.exit(applicationContext, { 0 })
      exitProcess(0)
    }
  }

  @EventListener(ApplicationFailedEvent::class)
  fun handleApplicationFailed(event: ApplicationFailedEvent) {
    if (internalProperties.stopRightAfterStart) {
      log.info("Exiting: StopRightAfterStart property is set to true")
      var exitStatus = 1
      if (event.exception.javaClass.name
          .contains("SQLGrammarException")
      ) {
        exitStatus = 0
      }

      SpringApplication.exit(applicationContext, { exitStatus })
      exitProcess(exitStatus)
    }
  }
}
