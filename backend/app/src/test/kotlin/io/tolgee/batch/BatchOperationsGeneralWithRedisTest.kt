package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.fixtures.RedisRunner
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration.Companion.JOB_QUEUE_TOPIC
import io.tolgee.testing.assert
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
@ContextConfiguration(initializers = [BatchOperationsGeneralWithRedisTest.Companion.Initializer::class])
class BatchOperationsGeneralWithRedisTest : AbstractBatchOperationsGeneralTest() {
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
    redisRunner.stop()
  }

  @Test
  fun `removes from queue using event`() {
    var done = false
    whenever(translationBatchProcessor.process(any(), any())).thenAnswer {
      while (!done) {
        Thread.sleep(100)
      }
    }
    try {

      runJob(keyCount = 200)

      waitForNotThrowing {
        val peek = batchJobActionService.queue.peek()
        peek.assert.isNotNull
      }

      val peek = batchJobActionService.queue.peek()
      batchJobActionService.queue.contains(peek).assert.isTrue()
      Mockito.clearInvocations(redisTemplate)
      batchJobActionService.publishRemoveConsuming(peek)
      verify(redisTemplate, times(1))
        .convertAndSend(
          eq(JOB_QUEUE_TOPIC),
          eq(
            jacksonObjectMapper().writeValueAsString(
              JobQueueItemEvent(peek, QueueItemType.REMOVE)
            )
          )
        )
      batchJobActionService.queue.contains(peek).assert.isFalse()
    } finally {
      done = true
    }
  }
}
