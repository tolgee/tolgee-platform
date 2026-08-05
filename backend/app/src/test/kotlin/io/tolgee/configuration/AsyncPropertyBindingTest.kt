package io.tolgee.configuration

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * The deployment values.yaml pins tolgee.async.* explicitly, so those keys have to survive relaxed
 * binding all the way into the executors — nothing else in the suite would notice a rename.
 */
@SpringBootTest(
  properties = [
    "tolgee.async.streaming.max-threads = 7",
    "tolgee.async.streaming.queue-capacity = 9",
    "tolgee.async.streaming.keep-alive-seconds = 30",
    "tolgee.async.background.max-threads = 5",
  ],
)
class AsyncPropertyBindingTest {
  @Autowired
  @Qualifier(AsyncWebMvcConfiguration.STREAMING_EXECUTOR_BEAN_NAME)
  private lateinit var streamingAsyncExecutor: ThreadPoolTaskExecutor

  @Autowired
  private lateinit var asyncMethodConfiguration: AsyncMethodConfiguration

  @Test
  fun `binds the streaming pool from configuration`() {
    streamingAsyncExecutor.corePoolSize.assert.isEqualTo(7)
    streamingAsyncExecutor.maxPoolSize.assert.isEqualTo(7)
    streamingAsyncExecutor.keepAliveSeconds.assert.isEqualTo(30)
    streamingAsyncExecutor.threadPoolExecutor.queue
      .remainingCapacity()
      .assert
      .isEqualTo(9)
  }

  @Test
  fun `binds the background pool from configuration`() {
    asyncMethodConfiguration
      .backgroundAsyncExecutor()
      .corePoolSize.assert
      .isEqualTo(5)
  }
}
