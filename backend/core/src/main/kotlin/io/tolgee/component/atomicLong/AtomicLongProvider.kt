package io.tolgee.component.atomicLong

import io.tolgee.component.UsingRedisProvider
import io.tolgee.util.Logging
import io.tolgee.util.TolgeeAtomicLong
import io.tolgee.util.logger
import org.redisson.api.RedissonClient
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class AtomicLongProvider(
  val isUsingRedisProvider: UsingRedisProvider,
  val applicationContext: ApplicationContext,
) : Logging {
  fun get(
    name: String,
    defaultProvider: () -> Long,
  ): TolgeeAtomicLong {
    return if (isUsingRedisProvider.areWeUsingRedis) {
      // we need to lock it, because we don't want to set the default multiple times
      val lock = redissonClient.getLock("lock_$name")
      try {
        lock.lock(10, TimeUnit.SECONDS)
        logger.debug("Acquired lock for $name")
        val atomicLong = redissonClient.getAtomicLong(name)
        if (!atomicLong.isExists) {
          atomicLong.set(defaultProvider())
        }
        RedisTolgeeAtomicLong(atomicLong)
      } finally {
        if (lock.isHeldByCurrentThread) {
          lock.unlock()
        }
      }
    } else {
      MemoryTolgeeAtomicLong(name, defaultProvider)
    }
  }

  val redissonClient: RedissonClient by lazy {
    applicationContext.getBean(RedissonClient::class.java)
  }
}
