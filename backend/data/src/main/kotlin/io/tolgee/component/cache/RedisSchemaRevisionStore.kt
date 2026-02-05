package io.tolgee.component.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * Redis-based implementation of [SchemaRevisionStore].
 *
 * Stores schema revisions as a JSON object in a Redis key, suitable for
 * distributed deployments where multiple instances share the same Redis.
 *
 * This implementation is used when Redis is available; otherwise
 * [LocalSchemaRevisionStore] is used as a fallback.
 */
@Component
@Primary
@ConditionalOnBean(StringRedisTemplate::class)
class RedisSchemaRevisionStore(
  private val redisTemplate: StringRedisTemplate,
  private val objectMapper: ObjectMapper,
) : SchemaRevisionStore,
  Logging {
  companion object {
    private const val REVISIONS_KEY = "tolgee:cache:schema_revisions"
  }

  override fun getStoredRevisions(): Map<String, Int> {
    return try {
      val json = redisTemplate.opsForValue().get(REVISIONS_KEY)
      if (json.isNullOrBlank()) {
        emptyMap()
      } else {
        objectMapper.readValue(json)
      }
    } catch (e: Exception) {
      logger.warn("Failed to read schema revisions from Redis", e)
      emptyMap()
    }
  }

  override fun storeRevisions(revisions: Map<String, Int>) {
    try {
      val json = objectMapper.writeValueAsString(revisions)
      redisTemplate.opsForValue().set(REVISIONS_KEY, json)
    } catch (e: Exception) {
      logger.warn("Failed to store schema revisions to Redis", e)
    }
  }
}
