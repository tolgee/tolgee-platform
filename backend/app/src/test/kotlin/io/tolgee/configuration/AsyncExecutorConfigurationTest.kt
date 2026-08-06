package io.tolgee.configuration

import com.zaxxer.hikari.HikariDataSource
import io.tolgee.Metrics
import io.tolgee.component.automations.AutomationActivityListener
import io.tolgee.events.OnProjectActivityStoredEvent
import io.tolgee.exceptions.StreamingCapacityExceededException
import io.tolgee.exceptions.StreamingUnavailableException
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
  @Qualifier(AsyncWebMvcConfiguration.STREAMING_EXECUTOR_BEAN_NAME)
  private lateinit var streamingAsyncExecutor: ThreadPoolTaskExecutor

  @Autowired
  private lateinit var asyncMethodConfiguration: AsyncMethodConfiguration

  @Autowired
  @Qualifier(AsyncMethodConfiguration.BACKGROUND_EXECUTOR_BEAN_NAME)
  private lateinit var backgroundAsyncExecutor: ThreadPoolTaskExecutor

  @Test
  fun `derives both pools from the database connection pool`() {
    (dataSource as HikariDataSource)
      .maximumPoolSize.assert
      .describedAs("test connection pool size the expectations below are pinned to")
      .isEqualTo(100)

    asyncExecutorFactory.connectionPoolSize.assert.isEqualTo(100)
    asyncExecutorFactory.streamingMaxThreads.assert.isEqualTo(33)
    asyncExecutorFactory.streamingQueueCapacity.assert.isEqualTo(AsyncExecutorFactory.MIN_QUEUE_CAPACITY)
    asyncExecutorFactory.backgroundMaxThreads.assert.isEqualTo(16)
  }

  @Test
  fun `no pool is left at the single-threaded default`() {
    listOf(streamingAsyncExecutor, backgroundAsyncExecutor).forEach { executor ->
      executor.corePoolSize.assert.isEqualTo(executor.maxPoolSize)
      executor.corePoolSize.assert.isGreaterThan(1)
    }
  }

  @Test
  fun `streaming queue is bounded and background queue is not`() {
    streamingAsyncExecutor.threadPoolExecutor.queue
      .remainingCapacity()
      .assert
      .isEqualTo(AsyncExecutorFactory.MIN_QUEUE_CAPACITY)
    asyncMethodConfiguration
      .backgroundAsyncExecutor()
      .threadPoolExecutor.queue
      .remainingCapacity()
      .assert
      .isEqualTo(Int.MAX_VALUE)
  }

  @Test
  fun `the Async executor is the background bean itself`() {
    asyncMethodConfiguration.asyncExecutor.assert.isSameAs(backgroundAsyncExecutor)
  }

  /** Without these, context close rejects in-flight submissions into their callers. */
  @Test
  fun `executors keep accepting work while the context closes`() {
    listOf(
      streamingAsyncExecutor,
      backgroundAsyncExecutor,
      asyncMethodConfiguration.websocketAsyncExecutor(),
      asyncMethodConfiguration.automationAsyncExecutor(),
    ).forEach { executor ->
      ReflectionTestUtils
        .getField(executor, "acceptTasksAfterContextClose")
        .assert
        .isEqualTo(true)
    }
  }

  /** Background work is worth draining; a queued stream's client is already gone. */
  @Test
  fun `only the background pools drain on shutdown`() {
    ReflectionTestUtils
      .getField(streamingAsyncExecutor, "waitForTasksToCompleteOnShutdown")
      .assert
      .isEqualTo(false)
    ReflectionTestUtils
      .getField(backgroundAsyncExecutor, "waitForTasksToCompleteOnShutdown")
      .assert
      .isEqualTo(true)
  }

  @Test
  fun `pools have distinguishable thread names`() {
    setOf(
      AsyncExecutorFactory.STREAMING_THREAD_NAME_PREFIX,
      AsyncExecutorFactory.BACKGROUND_THREAD_NAME_PREFIX,
      AsyncExecutorFactory.WEBSOCKET_THREAD_NAME_PREFIX,
    ).assert.hasSize(3)

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

  /** Its debounce is a find-then-insert with no lock, so two revisions at once defeat it. */
  @Test
  fun `automation triggers are pinned to a serial executor`() {
    val automation = asyncMethodConfiguration.automationAsyncExecutor()
    automation.corePoolSize.assert.isEqualTo(1)
    automation.maxPoolSize.assert.isEqualTo(1)
    automation.threadNamePrefix.assert.isEqualTo(AsyncExecutorFactory.AUTOMATION_THREAD_NAME_PREFIX)
    automation.threadPoolExecutor.queue
      .remainingCapacity()
      .assert
      .isEqualTo(Int.MAX_VALUE)

    AutomationActivityListener::class.java.declaredMethods
      .filter { it.name == "listen" }
      .assert
      .hasSize(2)
    AutomationActivityListener::class.java.declaredMethods
      .filter { it.name == "listen" }
      .forEach {
        it
          .getAnnotation(Async::class.java)
          .value.assert
          .isEqualTo(AsyncMethodConfiguration.AUTOMATION_EXECUTOR_BEAN_NAME)
      }
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

  /** core == max, so core-thread timeout is the only thing that ever releases a pooled thread. */
  @Test
  fun `pooled threads are released when idle`() {
    streamingAsyncExecutor.threadPoolExecutor
      .allowsCoreThreadTimeOut()
      .assert
      .isTrue()
    backgroundAsyncExecutor.threadPoolExecutor
      .allowsCoreThreadTimeOut()
      .assert
      .isTrue()
  }

  @Test
  fun `the task decorator survives construction`() {
    ReflectionTestUtils
      .getField(streamingAsyncExecutor, "taskDecorator")
      .assert
      .isInstanceOf(CompositeTaskDecorator::class.java)
  }

  @Test
  fun `streaming pool actually runs tasks in parallel`() {
    assertRunsInParallel(streamingAsyncExecutor)
  }

  @Test
  fun `background pool actually runs tasks in parallel`() {
    assertRunsInParallel(backgroundAsyncExecutor)
  }

  @Test
  fun `the streaming bean itself carries the rejecting policy`() {
    streamingAsyncExecutor.threadPoolExecutor.rejectedExecutionHandler.assert
      .isInstanceOf(StreamingAbortPolicy::class.java)
  }

  @Test
  fun `a shutting-down pool is not reported as saturation`() {
    var counted = false
    val policy = StreamingAbortPolicy { counted = true }
    val executor = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, java.util.concurrent.SynchronousQueue())
    executor.shutdown()

    assertThatThrownBy { policy.rejectedExecution({}, executor) }
      .isInstanceOf(StreamingUnavailableException::class.java)
      .isNotInstanceOf(StreamingCapacityExceededException::class.java)
    counted.assert.isFalse()
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
