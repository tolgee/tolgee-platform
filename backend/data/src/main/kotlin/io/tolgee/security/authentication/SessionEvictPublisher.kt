package io.tolgee.security.authentication

import io.tolgee.component.UsingRedisProvider
import io.tolgee.pubSub.RedisPubSubReceiverConfiguration
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class SessionEvictPublisher(
  private val usingRedisProvider: UsingRedisProvider,
  @Lazy
  private val redisTemplate: StringRedisTemplate,
  private val userSessionHotCache: UserSessionHotCache,
) : Logging {
  /**
   * Tells every instance to drop its cached revocation state for the device. Failing to reach Redis
   * must not fail the revocation that already committed - the other instances still converge once
   * their cache entries expire.
   */
  fun publish(deviceId: String) {
    if (!usingRedisProvider.areWeUsingRedis) {
      return
    }

    try {
      redisTemplate.convertAndSend(
        RedisPubSubReceiverConfiguration.SESSION_EVICT_TOPIC,
        jacksonObjectMapper().writeValueAsString(deviceId),
      )
    } catch (e: Exception) {
      logger.warn("Failed to publish session eviction for device $deviceId", e)
    }
  }

  @EventListener(SessionEvictEvent::class)
  fun onSessionEvict(event: SessionEvictEvent) {
    userSessionHotCache.evict(event.deviceId)
  }
}
