package io.tolgee.jobs.migration.translationStats

import io.tolgee.repository.TranslationRepository
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
      RepositoryItemReader<StatsMigrationTranslationView>().apply {
        setRepository(translationRepository)
        setMethodName(translationRepository::findAllForStatsUpdate.name)
        setSort(mapOf("id" to Sort.Direction.ASC))
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
