package io.tolgee.jobs.migration.allOrganizationOwner

import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import jakarta.persistence.EntityManager
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort.Direction
import org.springframework.transaction.PlatformTransactionManager

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class AllOrganizationOwnerJobConfiguration {
  companion object {
    const val JOB_NAME = "allOrganizationOwnerJob"
    const val STEP_SIZE = 100
  }

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var projectRepository: ProjectRepository

  @Autowired
  lateinit var organizationRepository: OrganizationRepository

  @Autowired
  lateinit var organizationService: OrganizationService

  @Autowired
  lateinit var projectService: ProjectService

  @Autowired
  lateinit var permissionService: PermissionService

  @Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @Autowired
  lateinit var platformTransactionManager: PlatformTransactionManager

  @Bean(JOB_NAME)
  fun job(jobRepository: JobRepository): Job {
    return JobBuilder(JOB_NAME, jobRepository)
      .flow(getNoOrgProjectsStep(jobRepository))
      .next(getNoRoleUserStep(jobRepository))
      .end()
      .build()
  }

  val noOrgProjectReader: ItemReader<Project>
    get() =
      RepositoryItemReader<Project>().apply {
        setRepository(projectRepository)
        setMethodName(projectRepository::findAllWithUserOwner.name)
        setSort(mapOf("id" to Direction.ASC))
        setPageSize(STEP_SIZE)
      }

  val noOrgProjectWriter: ItemWriter<Project> =
    ItemWriter { items ->
      items.forEach { project ->
        val organization =
          organizationRepository.findUsersDefaultOrganization(project.userOwner!!)
            ?: let {
              val ownerName = project.userOwner!!.name
              val ownerNameSafe = if (ownerName.length >= 3) ownerName else "$ownerName Organization"
              organizationService.create(
                OrganizationDto(name = ownerNameSafe),
                project.userOwner!!,
              )
            }

        val permission =
          permissionService.find(
            projectId = project.id,
            userId = project.userOwner!!.id,
          )
        permission?.let { permissionService.delete(it.id) }

        project.organizationOwner = organization
        project.userOwner = null
        projectService.save(project)
      }
    }

  fun getNoOrgProjectsStep(jobRepository: JobRepository): Step {
    return StepBuilder("noOrProjectStep", jobRepository)
      .chunk<Project, Project>(STEP_SIZE, platformTransactionManager)
      .reader(noOrgProjectReader)
      .writer(noOrgProjectWriter)
      .build()
  }

  val noRoleUserReader: ItemReader<UserAccount>
    get() =
      RepositoryItemReader<UserAccount>().apply {
        setRepository(userAccountRepository)
        setMethodName(userAccountRepository::findAllWithoutAnyOrganization.name)
        setSort(mapOf("id" to Direction.ASC))
        setPageSize(STEP_SIZE)
      }

  val noRoleUserWriter: ItemWriter<UserAccount> =
    ItemWriter { items ->
      items.forEach { userAccount ->
        organizationService.create(OrganizationDto(name = userAccount.name), userAccount)
      }
    }

  fun getNoRoleUserStep(jobRepository: JobRepository): Step =
    StepBuilder("noRoleUserStep", jobRepository)
      .chunk<UserAccount, UserAccount>(STEP_SIZE, platformTransactionManager)
      .reader(noRoleUserReader)
      .writer(noRoleUserWriter)
      .build()
}
