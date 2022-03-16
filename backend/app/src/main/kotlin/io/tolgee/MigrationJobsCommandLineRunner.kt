package io.tolgee

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class MigrationJobsCommandLineRunner(
  val tolgeeProperties: TolgeeProperties,
  @Qualifier("translationStatsJob")
  val translationStatsJob: Job,
  val jobLauncher: JobLauncher
) :
  CommandLineRunner, ApplicationListener<ContextClosedEvent> {
  override fun run(vararg args: String) {
    jobLauncher.run(translationStatsJob, JobParameters())
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {
  }
}
