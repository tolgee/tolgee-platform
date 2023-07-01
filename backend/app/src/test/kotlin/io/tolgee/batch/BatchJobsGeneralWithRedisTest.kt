package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.fixtures.RedisRunner
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration.Companion.JOB_QUEUE_TOPIC
import io.tolgee.testing.assert
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "spring.redis.port=56379",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [BatchJobsGeneralWithRedisTest.Companion.Initializer::class])
class BatchJobsGeneralWithRedisTest : AbstractBatchJobsGeneralTest() {
  companion object {
    val redisRunner = RedisRunner()

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }
  }

  @SpyBean
  @Autowired
  lateinit var redisTemplate: StringRedisTemplate

  @AfterAll
  fun cleanup() {
    Mockito.reset(redisTemplate)
    redisRunner.stop()
  }

  @Test
  fun `removes from queue using event`() {
    var done = false

    runBlocking {
      whenever(translationChunkProcessor.process(any(), any(), any(), any())).thenAnswer {
        while (!done) {
          Thread.sleep(100)
        }
      }
    }

    try {
      runChunkedJob(keyCount = 200)

      waitForNotThrowing {
        val peek = jobChunkExecutionQueue.peek()
        peek.assert.isNotNull
      }

      val peek = jobChunkExecutionQueue.peek()
      jobChunkExecutionQueue.contains(peek).assert.isTrue()
      Mockito.clearInvocations(redisTemplate)
      batchJobActionService.publishRemoveConsuming(peek)
      verify(redisTemplate, times(1))
        .convertAndSend(
          eq(JOB_QUEUE_TOPIC),
          eq(
            jacksonObjectMapper().writeValueAsString(
              JobQueueItemsEvent(listOf(peek), QueueEventType.REMOVE)
            )
          )
        )
      waitForNotThrowing(timeout = 2000) {
        jobChunkExecutionQueue.contains(peek).assert.isFalse()
      }
    } finally {
      done = true
    }
  }
}
