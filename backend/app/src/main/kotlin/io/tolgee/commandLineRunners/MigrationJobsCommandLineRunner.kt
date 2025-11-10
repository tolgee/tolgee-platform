package io.tolgee.commandLineRunners

import io.tolgee.jobs.migration.MigrationJobRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class MigrationJobsCommandLineRunner(
  val jobRunners: List<MigrationJobRunner>,
) : CommandLineRunner,
  ApplicationListener<ContextClosedEvent> {
  override fun run(vararg args: String) {
    jobRunners.forEach { it.run() }
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
  }
}
