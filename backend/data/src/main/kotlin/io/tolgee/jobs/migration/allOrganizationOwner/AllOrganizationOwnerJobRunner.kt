package io.tolgee.jobs.migration.allOrganizationOwner

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.jobs.migration.MigrationJobRunner
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.parameters.JobParameter
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class AllOrganizationOwnerJobRunner(
  val tolgeeProperties: TolgeeProperties,
  @Qualifier(AllOrganizationOwnerJobConfiguration.JOB_NAME)
  val job: Job,
  val jobOperator: JobOperator,
  val projectRepository: ProjectRepository,
  val userAccountRepository: UserAccountRepository,
  val jobRepository: JobRepository,
) : MigrationJobRunner {
  val log = LoggerFactory.getLogger(this::class.java)

  override fun run(): JobExecution? {
    val params = getJobParams()

    if (params != null) {
      return jobRepository.getLastJobExecution(AllOrganizationOwnerJobConfiguration.JOB_NAME, params)
        ?: return jobOperator.start(job, params)
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
    return JobParameters(setOf(JobParameter("idsHash", hash, String::class.java)))
  }
}
