package io.tolgee.configuration

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.Async
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(AsyncDispatchTargetTest.AsyncProbeConfiguration::class)
class AsyncDispatchTargetTest {
  @Autowired
  private lateinit var probe: AsyncProbe

  @Test
  fun `an unqualified Async method runs on the background pool`() {
    threadNameOf(probe.onDefaultExecutor())
      .assert
      .startsWith(AsyncExecutorFactory.BACKGROUND_THREAD_NAME_PREFIX)
  }

  @Test
  fun `the websocket qualifier resolves to the websocket pool`() {
    threadNameOf(probe.onWebsocketExecutor())
      .assert
      .startsWith(AsyncExecutorFactory.WEBSOCKET_THREAD_NAME_PREFIX)
  }

  private fun threadNameOf(future: CompletableFuture<String>): String = future.get(10, TimeUnit.SECONDS)

  @TestConfiguration
  class AsyncProbeConfiguration {
    @Bean
    fun asyncProbe() = AsyncProbe()
  }

  open class AsyncProbe {
    @Async
    open fun onDefaultExecutor(): CompletableFuture<String> =
      CompletableFuture.completedFuture(Thread.currentThread().name)

    @Async(AsyncMethodConfiguration.WEBSOCKET_EXECUTOR_BEAN_NAME)
    open fun onWebsocketExecutor(): CompletableFuture<String> =
      CompletableFuture.completedFuture(Thread.currentThread().name)
  }
}
