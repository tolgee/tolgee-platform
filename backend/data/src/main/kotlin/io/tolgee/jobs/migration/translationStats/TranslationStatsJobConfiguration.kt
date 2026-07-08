package io.tolgee.jobs.migration.translationStats

import io.tolgee.repository.TranslationRepository
import jakarta.persistence.EntityManager
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemReader
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class TranslationStatsJobConfiguration {
  companion object {
    const val JOB_NAME = "translationStatsJob"
  }

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var translationRepository: TranslationRepository

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var jobRepository: JobRepository

  @Autowired
  lateinit var platformTransactionManager: PlatformTransactionManager

  @Bean(JOB_NAME)
  fun translationStatsJob(): Job {
    return JobBuilder(JOB_NAME, jobRepository)
      .flow(step)
      .end()
      .build()
  }

  val reader: ItemReader<StatsMigrationTranslationView>
    get() =
      RepositoryItemReader<StatsMigrationTranslationView>(
        translationRepository,
        mapOf("id" to Sort.Direction.ASC),
      ).apply {
        setMethodName(translationRepository::findAllForStatsUpdate.name)
        setPageSize(100)
      }

  val writer: ItemWriter<TranslationStats> =
    ItemWriter { items ->
      items.forEach {
        val query =
          entityManager.createNativeQuery(
            "UPDATE translation set word_count = :wordCount, character_count = :characterCount where id = :id",
          )
        query.setParameter("wordCount", it.wordCount)
        query.setParameter("characterCount", it.characterCount)
        query.setParameter("id", it.id)
        query.executeUpdate()
      }
    }

  val step: Step
    get() =
      StepBuilder("step", jobRepository)
        .chunk<StatsMigrationTranslationView, TranslationStats>(100, platformTransactionManager)
        .reader(reader)
        .processor(TranslationProcessor())
        .writer(writer)
        .build()
}
