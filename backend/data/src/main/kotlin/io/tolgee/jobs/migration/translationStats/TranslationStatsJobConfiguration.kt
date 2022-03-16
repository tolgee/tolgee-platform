package io.tolgee.jobs.migration.translationStats

import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.batch.item.data.RepositoryItemWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import javax.persistence.EntityManager

@Configuration
class TranslationStatsJobConfiguration {
  @Autowired
  lateinit var jobBuilderFactory: JobBuilderFactory

  @Autowired
  lateinit var stepBuilderFactory: StepBuilderFactory

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var translationRepository: TranslationRepository

  @Bean("translationStatsJob")
  fun translationStatsJob(): Job {
    return jobBuilderFactory["translationStats"]
      .flow(step)
      .end()
      .build()
  }

  val reader: RepositoryItemReader<Translation>
    get() = RepositoryItemReader<Translation>().apply {
      setRepository(translationRepository)
      setMethodName("findAll")
      setSort(mapOf("id" to Sort.Direction.ASC))
      setPageSize(10)
    }

  val writer: RepositoryItemWriter<Translation>
    get() = RepositoryItemWriter<Translation>().apply {
      setRepository(translationRepository)
      setMethodName("save")
    }

  val step: Step
    get() = stepBuilderFactory["step"]
      .chunk<Translation, Translation>(100)
      .reader(reader)
      .processor(TranslationProcessor())
      .writer(writer)
      .build()
}
