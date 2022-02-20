package io.tolgee

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class PopulateCommandLineRunner(
  private val properties: TolgeeProperties,
  private val populator: DbPopulatorReal,
) :
  CommandLineRunner, ApplicationListener<ContextClosedEvent> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun run(vararg args: String) {
    logger.info("Populating DB with initial data...")
    if (properties.internal.populate) {
      populator.autoPopulate()
    }
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
    // we don't need this
  }
}
