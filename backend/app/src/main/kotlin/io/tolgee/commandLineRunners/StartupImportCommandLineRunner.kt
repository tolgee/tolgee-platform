package io.tolgee.commandLineRunners

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.service.StartupImportService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.core.Ordered
import org.springframework.stereotype.Component

@Component
class StartupImportCommandLineRunner(
  val tolgeeProperties: TolgeeProperties,
  val startupImportService: StartupImportService,
) : CommandLineRunner,
  ApplicationListener<ContextClosedEvent>,
  Ordered {
  override fun run(vararg args: String) {
    startupImportService.importFiles()
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
    // we don't need to do anything on context close
  }

  override fun getOrder(): Int {
    return 2
  }
}
