package io.tolgee.configuration

import com.zaxxer.hikari.HikariDataSource
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import javax.sql.DataSource

/**
 * Plain unit test so the floor and the "not a Hikari pool" fallback are reachable — a @SpringBootTest
 * is stuck with whatever connection pool the test context happens to have.
 */
class AsyncPoolSizeDerivationTest {
  @Test
  fun `derives each pool by its divisor`() {
    val factory = factory(connectionPoolSize = 60)

    factory.streamingMaxThreads.assert.isEqualTo(20)
    factory.backgroundMaxThreads.assert.isEqualTo(10)
  }

  @Test
  fun `never sizes a pool below the floor`() {
    val factory = factory(connectionPoolSize = 6)

    factory.streamingMaxThreads.assert.isEqualTo(AsyncExecutorFactory.MIN_POOL_SIZE)
    factory.backgroundMaxThreads.assert.isEqualTo(AsyncExecutorFactory.MIN_POOL_SIZE)
  }

  @Test
  fun `explicit configuration wins over the derivation`() {
    val properties = TolgeeProperties()
    properties.async.streaming.maxThreads = 7
    properties.async.streaming.queueCapacity = 3
    properties.async.background.maxThreads = 5

    val factory = factory(connectionPoolSize = 60, properties = properties)

    factory.streamingMaxThreads.assert.isEqualTo(7)
    factory.streamingQueueCapacity.assert.isEqualTo(3)
    factory.backgroundMaxThreads.assert.isEqualTo(5)
  }

  @Test
  fun `a queue capacity of zero is honoured rather than treated as auto`() {
    val properties = TolgeeProperties()
    properties.async.streaming.queueCapacity = 0

    factory(connectionPoolSize = 60, properties = properties).streamingQueueCapacity.assert.isEqualTo(0)
  }

  @Test
  fun `falls back to a fixed size when the DataSource cannot report a pool`() {
    val factory =
      AsyncExecutorFactory(TolgeeProperties(), providerOf<DataSource>(null))

    factory.connectionPoolSize.assert.isNull()
    // Fallback pool of 10 / streaming divisor of 3.
    factory.streamingMaxThreads.assert.isEqualTo(3)
  }

  @Test
  fun `a non-positive keep-alive does not produce an executor the JDK refuses`() {
    val executor =
      factory(connectionPoolSize = 60).create(
        threadNamePrefix = "test-keepalive-",
        maxThreads = 2,
        queueCapacity = 1,
        keepAliveSeconds = 0,
      )

    executor.initialize()
    try {
      executor.threadPoolExecutor
        .allowsCoreThreadTimeOut()
        .assert
        .isFalse()
    } finally {
      executor.shutdown()
    }
  }

  private fun factory(
    connectionPoolSize: Int,
    properties: TolgeeProperties = TolgeeProperties(),
  ): AsyncExecutorFactory {
    val dataSource = HikariDataSource()
    dataSource.maximumPoolSize = connectionPoolSize
    return AsyncExecutorFactory(properties, providerOf(dataSource))
  }
}
