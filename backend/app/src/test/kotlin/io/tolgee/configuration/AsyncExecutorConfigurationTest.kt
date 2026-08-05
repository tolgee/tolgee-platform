package io.tolgee.configuration

import com.zaxxer.hikari.HikariDataSource
import io.tolgee.Metrics
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.exceptions.StreamingCapacityExceededException
import io.tolgee.testing.assert
import io.tolgee.websocket.ActivityWebsocketListener
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@SpringBootTest
class AsyncExecutorConfigurationTest {
  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var asyncExecutorFactory: AsyncExecutorFactory

  @Autowired
  private lateinit var metrics: Metrics

  @Autowired
  private lateinit var requestMappingHandlerAdapter: RequestMappingHandlerAdapter

  @Autowired
  private lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  @Qualifier(AsyncWebMvcConfiguration.STREAMING_EXECUTOR_BEAN_NAME)
  private lateinit var streamingAsyncExecutor: ThreadPoolTaskExecutor

  @Autowired
  private lateinit var asyncMethodConfiguration: AsyncMethodConfiguration

  @Test
  fun `derives both pools from the database connection pool`() {
    (dataSource as HikariDataSource)
      .maximumPoolSize.assert
      .describedAs("test connection pool size the expectations below are pinned to")
      .isEqualTo(100)

    asyncExecutorFactory.connectionPoolSize.assert.isEqualTo(100)
    asyncExecutorFactory.streamingMaxThreads.assert.isEqualTo(33)
    asyncExecutorFactory.streamingQueueCapacity.assert.isEqualTo(33)
    asyncExecutorFactory.backgroundMaxThreads.assert.isEqualTo(16)
  }

  @Test
  fun `no pool is left at the single-threaded default`() {
    listOf(streamingAsyncExecutor, asyncMethodConfiguration.backgroundAsyncExecutor()).forEach { executor ->
      executor.corePoolSize.assert.isEqualTo(executor.maxPoolSize)
      executor.corePoolSize.assert.isGreaterThan(1)
    }
  }

  @Test
  fun `streaming queue is bounded and background queue is not`() {
    streamingAsyncExecutor.threadPoolExecutor.queue
      .remainingCapacity()
      .assert
      .isEqualTo(33)
    asyncMethodConfiguration
      .backgroundAsyncExecutor()
      .threadPoolExecutor.queue
      .remainingCapacity()
      .assert
      .isEqualTo(Int.MAX_VALUE)
  }

  @Test
  fun `pools have distinguishable thread names`() {
    streamingAsyncExecutor.threadNamePrefix.assert
      .isEqualTo(AsyncExecutorFactory.STREAMING_THREAD_NAME_PREFIX)
    asyncMethodConfiguration
      .backgroundAsyncExecutor()
      .threadNamePrefix.assert
      .isEqualTo(AsyncExecutorFactory.BACKGROUND_THREAD_NAME_PREFIX)
    asyncMethodConfiguration
      .websocketAsyncExecutor()
      .threadNamePrefix.assert
      .isEqualTo(AsyncExecutorFactory.WEBSOCKET_THREAD_NAME_PREFIX)
  }

  @Test
  fun `websocket executor stays serial so activity deltas keep their order`() {
    val websocket = asyncMethodConfiguration.websocketAsyncExecutor()
    websocket.corePoolSize.assert.isEqualTo(1)
    websocket.maxPoolSize.assert.isEqualTo(1)
    websocket.threadPoolExecutor.queue
      .remainingCapacity()
      .assert
      .isEqualTo(Int.MAX_VALUE)
  }

  @Test
  fun `activity broadcasts are pinned to the websocket executor`() {
    val onActivity =
      ActivityWebsocketListener::class.java
        .getDeclaredMethod("onActivity", OnProjectActivityStoredEvent::class.java)

    onActivity
      .getAnnotation(Async::class.java)
      .value.assert
      .isEqualTo(AsyncMethodConfiguration.WEBSOCKET_EXECUTOR_BEAN_NAME)
  }

  @Test
  fun `mvc async dispatch runs on the streaming executor`() {
    val installed = ReflectionTestUtils.getField(requestMappingHandlerAdapter, "taskExecutor")
    installed.assert.isInstanceOf(DelegatingSecurityContextAsyncTaskExecutor::class.java)

    val delegate = ReflectionTestUtils.getField(installed!!, "delegate")
    delegate.assert.isSameAs(streamingAsyncExecutor)
  }

  @Test
  fun `the task decorator survives construction`() {
    ReflectionTestUtils
      .getField(streamingAsyncExecutor, "taskDecorator")
      .assert
      .isInstanceOf(CompositeTaskDecorator::class.java)
  }

  @Test
  fun `the shipped defaults never trip the capacity warning`() {
    val poolSize = asyncExecutorFactory.connectionPoolSize!!
    val reserved =
      asyncExecutorFactory.streamingMaxThreads +
        asyncExecutorFactory.backgroundMaxThreads +
        tolgeeProperties.batch.concurrency

    reserved.assert.isLessThanOrEqualTo(poolSize - poolSize / AsyncCapacityReporter.SYNC_RESERVE_DIVISOR)
  }

  @Test
  fun `streaming pool actually runs tasks in parallel`() {
    assertRunsInParallel(streamingAsyncExecutor)
  }

  @Test
  fun `background pool actually runs tasks in parallel`() {
    assertRunsInParallel(asyncMethodConfiguration.backgroundAsyncExecutor())
  }

  @Test
  fun `the streaming bean itself carries the rejecting policy`() {
    streamingAsyncExecutor.threadPoolExecutor.rejectedExecutionHandler.assert
      .isInstanceOf(StreamingAbortPolicy::class.java)
  }

  @Test
  fun `saturated streaming pool rejects and counts the rejection`() {
    val rejectedBefore = metrics.streamingRejectedCounter.count()
    val executor =
      asyncExecutorFactory
        .create(
          threadNamePrefix = "test-reject-",
          maxThreads = 1,
          queueCapacity = 1,
          keepAliveSeconds = 60,
          rejectedExecutionHandler = StreamingAbortPolicy { metrics.streamingRejectedCounter.increment() },
        ).apply { initialize() }
    val release = CyclicBarrier(2)
    try {
      executor.submit { release.await(10, TimeUnit.SECONDS) }
      executor.submit { }

      assertThatThrownBy { executor.submit { } }
        .hasRootCauseInstanceOf(StreamingCapacityExceededException::class.java)
      metrics.streamingRejectedCounter
        .count()
        .assert
        .isEqualTo(rejectedBefore + 1)
    } finally {
      release.await(10, TimeUnit.SECONDS)
      executor.shutdown()
    }
  }

  /**
   * A barrier rather than a timing heuristic: if the pool is serial, no task can ever reach the
   * barrier's trip count and the await times out.
   */
  private fun assertRunsInParallel(executor: ThreadPoolTaskExecutor) {
    val poolExecutor: ThreadPoolExecutor = executor.threadPoolExecutor
    val parallelism = minOf(executor.maxPoolSize, MAX_TESTED_PARALLELISM)
    parallelism.assert.isGreaterThan(1)

    val barrier = CyclicBarrier(parallelism)
    val futures = (1..parallelism).map { poolExecutor.submit { barrier.await(10, TimeUnit.SECONDS) } }

    futures.forEach { it.get(15, TimeUnit.SECONDS) }
  }

  companion object {
    const val MAX_TESTED_PARALLELISM = 4
  }
}
