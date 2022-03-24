package io.tolgee

import io.tolgee.jobs.migration.translationStats.TranslationsStatsUpdateJobRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class MigrationJobsCommandLineRunner(
  private val translationsStatsUpdateJobRunner: TranslationsStatsUpdateJobRunner
) : CommandLineRunner, ApplicationListener<ContextClosedEvent> {

  override fun run(vararg args: String) {
    translationsStatsUpdateJobRunner.run()
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
  }
}
