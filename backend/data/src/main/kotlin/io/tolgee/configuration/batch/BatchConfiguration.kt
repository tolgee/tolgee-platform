package io.tolgee.configuration.batch

import io.tolgee.model.translation.Translation
import org.hibernate.SessionFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.core.ChunkListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.listener.JobExecutionListenerSupport
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.HibernateCursorItemReader
import org.springframework.batch.item.database.JpaItemWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.persistence.EntityManagerFactory


@Configuration
@EnableBatchProcessing
class BatchConfiguration(
  private val jobBuilderFactory: JobBuilderFactory,
  private val stepBuilderFactory: StepBuilderFactory,
  private val entityManagerFactory: EntityManagerFactory
) {
  val log = LoggerFactory.getLogger(BatchConfiguration::class.java)

  @Bean
  @StepScope
  fun reader(@Value("#{jobParameters[projectId]}") projectId: Long): HibernateCursorItemReader<Translation> {
    val reader = HibernateCursorItemReader<Translation>()
    reader.setQueryString("select t from Translation t join t.key k where k.project.id = :projectId ")
    reader.setParameterValues(mapOf("projectId" to projectId))
    reader.setSessionFactory(entityManagerFactory.unwrap(SessionFactory::class.java))
    return reader
  }

  @Bean
  fun processor(): ItemProcessor<Translation, Translation> {
    return ItemProcessor { translation ->
      log.info("working...")
      Thread.sleep(1000)
      translation.apply { text = "processed: $text" }
    }
  }

  @Bean
  fun writer(): ItemWriter<Translation> {
    val writer = JpaItemWriter<Translation>()
    writer.setEntityManagerFactory(entityManagerFactory)
    return writer
  }

  @Bean
  fun step1(writer: ItemWriter<Translation>, reader: ItemReader<Translation>): Step {
    val listener = object : ChunkListener {
      override fun beforeChunk(context: ChunkContext) {
        log.info("Working on chunk.")
      }

      override fun afterChunk(context: ChunkContext) {
        log.info("Chunk done.")
      }

      override fun afterChunkError(context: ChunkContext) {
        log.info("Chunk error.")
      }
    }

    return stepBuilderFactory["step1"]
      .chunk<Translation, Translation>(10)
      .reader(reader)
      .processor(processor())
      .writer(writer)
      .listener(listener)
      .build()
  }

  @Bean
  fun theJob(step1: Step): Job {
    val listener = object : JobExecutionListenerSupport() {
      override fun afterJob(jobExecution: JobExecution) {
        log.info("Done")
      }
    }

    return jobBuilderFactory["replaceInTranslation"]
      .incrementer(RunIdIncrementer())
      .listener(listener)
      .flow(step1)
      .end()
      .build()
  }
}
