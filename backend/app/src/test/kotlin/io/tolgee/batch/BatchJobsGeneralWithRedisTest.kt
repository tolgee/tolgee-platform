package io.tolgee.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.data.QueueEventType
import io.tolgee.batch.events.JobQueueItemsEvent
import io.tolgee.fixtures.RedisRunner
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration.Companion.JOB_QUEUE_TOPIC
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
    "spring.redis.port=56379",
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ContextConfiguration(initializers = [BatchJobsGeneralWithRedisTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextRecreatingTest
class BatchJobsGeneralWithRedisTest : AbstractBatchJobsGeneralTest() {
  companion object {
    val redisRunner = RedisRunner()

    @AfterAll
    @JvmStatic
    fun stopRedis() {
      redisRunner.stop()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }
  }

  @Autowired
  lateinit var jobConcurrentLauncher: BatchJobConcurrentLauncher

  @MockitoSpyBean
  @Autowired
  lateinit var redisTemplate: StringRedisTemplate

  @AfterEach
  fun cleanup() {
    Mockito.reset(redisTemplate)
    Mockito.clearInvocations(redisTemplate)
    jobConcurrentLauncher.pause = false
  }

  @Test
  fun `removes from queue using event`() {
    jobConcurrentLauncher.pause = true

    util.runChunkedJob(keyCount = 200)

    waitForNotThrowing {
      val peek = batchJobChunkExecutionQueue.peek()
      peek.assert.isNotNull
    }

    val peek = batchJobChunkExecutionQueue.peek()
    batchJobChunkExecutionQueue.contains(peek).assert.isTrue()
    Mockito.clearInvocations(redisTemplate)
    batchJobActionService.publishRemoveConsuming(peek)
    verify(redisTemplate, times(1))
      .convertAndSend(
        eq(JOB_QUEUE_TOPIC),
        eq(
          jacksonObjectMapper().writeValueAsString(
            JobQueueItemsEvent(listOf(peek), QueueEventType.REMOVE),
          ),
        ),
      )
    waitForNotThrowing(timeout = 2000) {
      batchJobChunkExecutionQueue.contains(peek).assert.isFalse()
    }
  }
}
