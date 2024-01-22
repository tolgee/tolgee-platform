package io.tolgee.jobs.migration.allOrganizationOwner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.jobs.migration.MigrationJobRunner
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
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
class AllOrganizationOwnerJobRunner(
  val tolgeeProperties: TolgeeProperties,
  @Qualifier(AllOrganizationOwnerJobConfiguration.JOB_NAME)
  val job: Job,
  val jobLauncher: JobLauncher,
  val projectRepository: ProjectRepository,
  val userAccountRepository: UserAccountRepository,
  val jobRepository: JobRepository,
) : MigrationJobRunner {
  val log = LoggerFactory.getLogger(this::class.java)

  override fun run(): JobExecution? {
    val params = getJobParams()

    if (params != null) {
      return jobRepository.getLastJobExecution(AllOrganizationOwnerJobConfiguration.JOB_NAME, params)
        ?: return jobLauncher.run(job, params)
    }
    return null
  }

  private fun getJobParams(): JobParameters? {
    val userIds = userAccountRepository.findAllWithoutAnyOrganizationIds()
    val projectIds = projectRepository.findAllWithUserOwnerIds()
    if (projectIds.isEmpty() && userIds.isEmpty()) {
      return null
    }
    val json = jacksonObjectMapper().writeValueAsBytes(mapOf("users" to userIds, "projects" to projectIds))
    val hash = DigestUtils.sha256Hex(json)
    return JobParameters(mapOf("idsHash" to JobParameter(hash, String::class.java)))
  }
}
