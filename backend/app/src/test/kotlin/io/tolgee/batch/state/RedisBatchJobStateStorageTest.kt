package io.tolgee.batch.state

import io.tolgee.AbstractSpringTest
import io.tolgee.fixtures.RedisRunner
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.use-redis=true",
    "tolgee.cache.enabled=true",
    "tolgee.websocket.use-redis=true",
  ],
)
@ContextConfiguration(initializers = [RedisBatchJobStateStorageTest.Companion.Initializer::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RedisBatchJobStateStorageTest : AbstractSpringTest() {
  companion object {
    private const val STATE_KEY_PREFIX = "batch_job_state:"
    private const val STATE_INITIALIZED_PREFIX = "batch_job_state_initialized:"
    private const val STARTED_PREFIX = "batch_job_started:"

    val redisRunner = RedisRunner()

    @AfterAll
    @JvmStatic
    fun stopRedis() {
      redisRunner.stop()
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
        TestPropertyValues
          .of("spring.data.redis.port=${RedisRunner.port}")
          .applyTo(configurableApplicationContext)
      }
    }
  }

  @Autowired
  lateinit var stateProvider: BatchJobStateProvider

  @MockitoSpyBean
  @Autowired
  lateinit var redissonClient: RedissonClient

  private val usedJobIds = mutableSetOf<Long>()

  @AfterEach
  fun cleanup() {
    usedJobIds.forEach { stateProvider.removeJobState(it) }
    usedJobIds.clear()
  }

  @Test
  fun `removeJobState pipelines its deletes into a single RBatch`() {
    val jobId =
      seed(900_020L, BatchJobChunkExecutionStatus.SUCCESS) {
        incrementRunningCount(it)
        tryMarkJobStarted(it)
      }

    clearInvocations(redissonClient)
    stateProvider.removeJobState(jobId)

    // The deletes must be pipelined through one RBatch, not issued as per-key round-trips.
    verify(redissonClient, times(1)).createBatch()
    verify(redissonClient, never()).getAtomicLong(any<String>())

    stateProvider.hasCachedJobState(jobId).assert.isFalse()
  }

  @Test
  fun `clearUnusedStates clears completed jobs across chunks and keeps counters and unfinished jobs`() {
    // Exceeds the production CLEANUP_BATCH_SIZE (100) so cleanup runs across more than one chunk.
    val completedJobIds = (901_000L until 901_150L).toList()
    completedJobIds.forEach {
      seed(it, BatchJobChunkExecutionStatus.SUCCESS) {
        incrementRunningCount(it)
        tryMarkJobStarted(it)
      }
    }
    val runningJobId = seed(900_002L, BatchJobChunkExecutionStatus.RUNNING)

    stateProvider.clearUnusedStates()

    // Every completed job's hash is cleared — including those in the second chunk.
    completedJobIds.forEach { stateProvider.hasCachedJobState(it).assert.isFalse() }

    // Counters and the started marker survive until removeJobState finalizes the job
    // (dropping the started marker would allow re-execution).
    val sample = completedJobIds.first()
    isInitializedBucketPresent(sample).assert.isFalse()
    stateProvider.getRunningCount(sample).assert.isEqualTo(1)
    isStartedBucketPresent(sample).assert.isTrue()

    // Unfinished job is left untouched.
    stateProvider.hasCachedJobState(runningJobId).assert.isTrue()
    isInitializedBucketPresent(runningJobId).assert.isTrue()
  }

  @Test
  fun `clearUnusedStates skips a job whose state cannot be read and still cleans healthy jobs`() {
    val healthyJobId = seed(900_040L, BatchJobChunkExecutionStatus.SUCCESS)
    val corruptJobId = 900_041L
    usedJobIds += corruptJobId
    // Forge a value the state codec cannot deserialize, so this job's read future fails.
    redissonClient.getMap<String, String>("$STATE_KEY_PREFIX$corruptJobId", StringCodec.INSTANCE)["1"] = "corrupt"
    redissonClient.getBucket<Boolean>("$STATE_INITIALIZED_PREFIX$corruptJobId").set(true)

    stateProvider.clearUnusedStates()

    // The unreadable job is skipped, not deleted...
    stateProvider.hasCachedJobState(corruptJobId).assert.isTrue()
    // ...while the healthy completed job in the same pass is still cleaned.
    stateProvider.hasCachedJobState(healthyJobId).assert.isFalse()
  }

  private fun seed(
    jobId: Long,
    status: BatchJobChunkExecutionStatus,
    counters: BatchJobStateProvider.(Long) -> Unit = {},
  ): Long {
    usedJobIds += jobId
    stateProvider.updateSingleExecution(jobId, 1L, executionState(status))
    redissonClient.getBucket<Boolean>("$STATE_INITIALIZED_PREFIX$jobId").set(true)
    stateProvider.counters(jobId)
    return jobId
  }

  private fun executionState(status: BatchJobChunkExecutionStatus) =
    ExecutionState(
      successTargetsCount = 0,
      status = status,
      chunkNumber = 0,
      retry = null,
      transactionCommitted = true,
    )

  private fun isInitializedBucketPresent(jobId: Long) =
    redissonClient.getBucket<Boolean>("$STATE_INITIALIZED_PREFIX$jobId").isExists

  private fun isStartedBucketPresent(jobId: Long) = redissonClient.getBucket<Boolean>("$STARTED_PREFIX$jobId").isExists
}
