package io.tolgee.ee.jobs.migration.translationMemoryBackfill

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.model.Project
import io.tolgee.repository.ProjectRepository
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
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
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager

/**
 * Spring Batch job that provisions project-type Translation Memories for legacy projects.
 *
 * Motivation: projects created before the TM management feature shipped have no project TM.
 * The feature-gained event listener only fires on plan transitions; orgs that were already
 * paid before the migration code existed never receive the event. This startup job closes
 * that gap by iterating all non-deleted projects without a project TM and creating one for
 * every project whose organization has the TRANSLATION_MEMORY feature enabled. On free-plan
 * orgs the writer short-circuits and leaves the project untouched.
 *
 * Backfill of existing translations into the newly created TM happens inside
 * [TranslationMemoryManagementService.createProjectTm].
 */
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class TranslationMemoryBackfillJobConfiguration {
  companion object {
    const val JOB_NAME = "translationMemoryBackfillJob"
    const val STEP_SIZE = 100
  }

  @Autowired
  lateinit var projectRepository: ProjectRepository

  @Autowired
  lateinit var translationMemoryManagementService: TranslationMemoryManagementService

  @Autowired
  lateinit var enabledFeaturesProvider: EnabledFeaturesProvider

  @Autowired
  lateinit var jobRepository: JobRepository

  @Autowired
  lateinit var platformTransactionManager: PlatformTransactionManager

  @Bean(JOB_NAME)
  fun job(): Job {
    return JobBuilder(JOB_NAME, jobRepository)
      .flow(step)
      .end()
      .build()
  }

  val reader: ItemReader<Project>
    get() =
      RepositoryItemReader<Project>().apply {
        setRepository(projectRepository)
        setMethodName(projectRepository::findAllWithoutProjectTm.name)
        setSort(mapOf("id" to Sort.Direction.ASC))
        setPageSize(STEP_SIZE)
      }

  val writer: ItemWriter<Project> =
    ItemWriter { items ->
      items.forEach { project ->
        val orgId = project.organizationOwner.id
        if (!enabledFeaturesProvider.isFeatureEnabled(orgId, Feature.TRANSLATION_MEMORY)) return@forEach
        translationMemoryManagementService.getOrCreateProjectTm(project.id)
      }
    }

  val step: Step
    get() =
      StepBuilder("translationMemoryBackfillStep", jobRepository)
        .chunk<Project, Project>(STEP_SIZE, platformTransactionManager)
        .reader(reader)
        .writer(writer)
        .build()
}
