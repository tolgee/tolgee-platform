package io.tolgee.jobs.migration.translationStats

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.jobs.migration.MigrationJobRunner
import io.tolgee.repository.TranslationRepository
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
class TranslationsStatsUpdateJobRunner(
  val tolgeeProperties: TolgeeProperties,
  @Qualifier(TranslationStatsJobConfiguration.JOB_NAME)
  val translationStatsJob: Job,
  val jobLauncher: JobLauncher,
  val translationRepository: TranslationRepository,
  val jobRepository: JobRepository,
) : MigrationJobRunner {
  val log = LoggerFactory.getLogger(this::class.java)

  override fun run(): JobExecution? {
    val params = getJobParams()

    if (params != null) {
      return jobRepository.getLastJobExecution(TranslationStatsJobConfiguration.JOB_NAME, params)
        ?: return jobLauncher.run(translationStatsJob, params)
    }
    return null
  }

  private fun getJobParams(): JobParameters? {
    val ids = translationRepository.findAllIdsForStatsUpdate()
    if (ids.isEmpty()) {
      return null
    }
    val hash = DigestUtils.sha256Hex(ids.flatMap { it.toBigInteger().toByteArray().toList() }.toByteArray())
    return JobParameters(mapOf("idsHash" to JobParameter(hash, String::class.java)))
  }
}
