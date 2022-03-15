package io.tolgee.socketio

import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.store.RedissonStoreFactory
import com.corundumstudio.socketio.store.StoreFactory
import io.tolgee.CleanDbBeforeClass
import io.tolgee.fixtures.RedisRunner
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "spring.redis.port=56379",
    "tolgee.socket-io.enabled=true",
    "tolgee.socket-io.use-redis=true"
  ]
)
@CleanDbBeforeClass
@ContextConfiguration(initializers = [SocketIoWithRedisTest.Companion.Initializer::class])
class SocketIoWithRedisTest : AbstractSocketIoTest() {
  companion object {
    const val SECONDARY_SOCKET_SERVER_PORT = 19091

    val redisRunner = RedisRunner()

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

      override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        redisRunner.run()
      }
    }
  }

  @Autowired
  lateinit var redissonClient: RedissonClient

  @AfterAll
  fun cleanup() {
    redisRunner.stop()
  }

  override fun beforePrepareSockets() {
    runSecondarySocketIoServer()
    socketPorts.add(SECONDARY_SOCKET_SERVER_PORT.toString())
  }

  private val projectProvider = mock(SocketIoProjectProvider::class.java)

  @Test
  fun `event is dispatched on key edit`() {
    assertKeyModify()
  }

  @Test
  fun `event is dispatched on key delete`() {
    assertKeyDelete()
  }

  @Test
  fun `event is dispatched on key create`() {
    assertKeyCreate()
  }

  @Test
  fun `event is dispatched on translation edit`() {
    assertTranslationModify()
  }

  @Test
  fun `event is dispatched on translation delete`() {
    assertTranslationDelete()
  }

  @Test
  fun `event is dispatched on translation create`() {
    assertTranslationCreate()
  }

  private fun runSecondarySocketIoServer() {
    Mockito.`when`(projectProvider.getProject(any())).then { project }
    val config = com.corundumstudio.socketio.Configuration()
    config.socketConfig.isReuseAddress = true
    config.port = SECONDARY_SOCKET_SERVER_PORT
    val redissonStoreFactory: StoreFactory = RedissonStoreFactory(redissonClient)
    config.storeFactory = redissonStoreFactory
    val server = SocketIOServer(config)
    server.start()
    TranslationsConnectionListener(server, projectProvider).listen()
  }
}
