package io.tolgee.ee.jobs.migration.translationMemoryBackfill

import io.tolgee.jobs.migration.MigrationJobRunner
import io.tolgee.repository.ProjectRepository
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParameter
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class TranslationMemoryBackfillJobRunner(
  @Qualifier(TranslationMemoryBackfillJobConfiguration.JOB_NAME)
  val job: Job,
  val jobLauncher: JobLauncher,
  val projectRepository: ProjectRepository,
  val jobRepository: JobRepository,
) : MigrationJobRunner {
  val log = LoggerFactory.getLogger(this::class.java)

  override fun run(): JobExecution? {
    val params = getJobParams() ?: return null
    return jobRepository.getLastJobExecution(TranslationMemoryBackfillJobConfiguration.JOB_NAME, params)
      ?: jobLauncher.run(job, params)
  }

  /**
   * Hashes the set of project IDs missing a project TM so the Spring Batch job only re-runs
   * when that set actually changes (e.g. a new legacy project appears or a previous run
   * migrated some of them). Returns `null` when there is nothing to migrate so the runner
   * skips the job entirely.
   */
  private fun getJobParams(): JobParameters? {
    val ids = projectRepository.findAllIdsWithoutProjectTm()
    if (ids.isEmpty()) return null
    val hash = DigestUtils.sha256Hex(ids.flatMap { it.toBigInteger().toByteArray().toList() }.toByteArray())
    return JobParameters(mapOf("idsHash" to JobParameter(hash, String::class.java)))
  }
}
