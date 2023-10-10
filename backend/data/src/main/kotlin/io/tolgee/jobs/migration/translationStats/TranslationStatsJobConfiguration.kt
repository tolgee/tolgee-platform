package io.tolgee.jobs.migration.translationStats

import io.tolgee.repository.TranslationRepository
import jakarta.persistence.EntityManager
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import javax.sql.DataSource

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class TranslationStatsJobConfiguration {

  companion object {
    const val JOB_NAME = "translationStatsJob"
  }

  @Autowired
  lateinit var jobBuilderFactory: JobBuilderFactory

  @Autowired
  lateinit var stepBuilderFactory: StepBuilderFactory

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var translationRepository: TranslationRepository

  @Autowired
  lateinit var dataSource: DataSource

  @Bean(JOB_NAME)
  fun translationStatsJob(): Job {
    return jobBuilderFactory[JOB_NAME]
      .flow(step)
      .end()
      .build()
  }

  val reader: ItemReader<StatsMigrationTranslationView>
    get() = RepositoryItemReader<StatsMigrationTranslationView>().apply {
      setRepository(translationRepository)
      setMethodName(translationRepository::findAllForStatsUpdate.name)
      setSort(mapOf("id" to Sort.Direction.ASC))
      setPageSize(100)
    }

  val writer: ItemWriter<TranslationStats> = ItemWriter { items ->
    items.forEach {
      val query = entityManager.createNativeQuery(
        "UPDATE translation set word_count = :wordCount, character_count = :characterCount where id = :id"
      )
      query.setParameter("wordCount", it.wordCount)
      query.setParameter("characterCount", it.characterCount)
      query.setParameter("id", it.id)
      query.executeUpdate()
    }
  }

  val step: Step
    get() = stepBuilderFactory["step"]
      .chunk<StatsMigrationTranslationView, TranslationStats>(100)
      .reader(reader)
      .processor(TranslationProcessor())
      .writer(writer)
      .build()
}
