package io.tolgee.component

import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Executes an action at most once per [duration] for a given [key].
 * Uses Redis SET NX EX when available, falls back to in-memory timestamps.
 */
@Component
class Debouncer(
  private val usingRedisProvider: UsingRedisProvider,
  private val applicationContext: ApplicationContext,
) {
  private val inMemoryTimestamps = ConcurrentHashMap<String, Long>()

  /**
   * Runs [fn] only if [key] hasn't been executed within [duration].
   * Returns the result of [fn], or null if debounced (skipped).
   */
  fun <T> debounce(
    key: String,
    duration: Duration,
    fn: () -> T,
  ): T? {
    if (!acquire(key, duration)) return null
    return fn()
  }

  private fun acquire(
    key: String,
    duration: Duration,
  ): Boolean {
    if (usingRedisProvider.areWeUsingRedis) {
      val redisTemplate = applicationContext.getBean<StringRedisTemplate>()
      return redisTemplate.opsForValue().setIfAbsent(key, "1", duration) == true
    }

    val now = System.currentTimeMillis()
    val lastRun = inMemoryTimestamps[key]
    if (lastRun != null && now - lastRun < duration.toMillis()) return false
    inMemoryTimestamps[key] = now
    return true
  }
}
