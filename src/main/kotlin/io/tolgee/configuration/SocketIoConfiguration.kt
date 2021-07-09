package io.tolgee.configuration

import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.store.RedissonStoreFactory
import com.corundumstudio.socketio.store.StoreFactory
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class SocketIoConfiguration(
  private val tolgeeProperties: TolgeeProperties,
  private val redissonClient: RedissonClient
) {
  @Bean
  fun socketIOServer(): SocketIOServer? {
    val config = com.corundumstudio.socketio.Configuration()
    config.socketConfig.isReuseAddress = true
    tolgeeProperties.socketIo.host?.let { config.hostname = it }
    config.port = tolgeeProperties.socketIo.port
    val redissonStoreFactory: StoreFactory = RedissonStoreFactory(redissonClient)
    config.storeFactory = redissonStoreFactory
    return SocketIOServer(config)
  }
}
