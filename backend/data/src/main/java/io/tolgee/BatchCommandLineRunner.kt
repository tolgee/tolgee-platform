package io.tolgee

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class BatchCommandLineRunner(
  private val jobLauncher: JobLauncher,
  private val job: Job
) :
  CommandLineRunner, ApplicationListener<ContextClosedEvent> {
  override fun run(vararg args: String) {
    val parameters = JobParametersBuilder()
      .addParameter("projectId", JobParameter(1))
      .addLong("aa", 3)
      .toJobParameters()

    this.jobLauncher.run(job, parameters)
  }

  override fun onApplicationEvent(event: ContextClosedEvent) {

  }
}
