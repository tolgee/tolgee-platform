package io.tolgee.commandLineRunners

import io.tolgee.jobs.migration.allOrganizationOwner.AllOrganizationOwnerJobRunner
import io.tolgee.jobs.migration.translationStats.TranslationsStatsUpdateJobRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class MigrationJobsCommandLineRunner(
  private val translationsStatsUpdateJobRunner: TranslationsStatsUpdateJobRunner,
  private val allOrganizationOwnerJobRunner: AllOrganizationOwnerJobRunner
) : CommandLineRunner, ApplicationListener<ContextClosedEvent> {

  override fun run(vararg args: String) {
    translationsStatsUpdateJobRunner.run()
    allOrganizationOwnerJobRunner.run()
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
  }
}
