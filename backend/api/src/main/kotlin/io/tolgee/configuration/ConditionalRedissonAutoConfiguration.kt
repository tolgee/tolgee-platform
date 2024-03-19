package io.tolgee.configuration

import org.redisson.Redisson
import org.redisson.spring.starter.RedissonAutoConfigurationV2
import org.redisson.spring.starter.RedissonProperties
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisOperations

@Configuration
@ConditionalOnClass(Redisson::class, RedisOperations::class)
@AutoConfigureBefore(RedisAutoConfiguration::class)
@EnableConfigurationProperties(RedissonProperties::class, RedisProperties::class)
@ConditionalOnExpression(
  "\${tolgee.websocket.use-redis:false} or " +
    "(\${tolgee.cache.use-redis:false} and \${tolgee.cache.enabled:false})",
)
class ConditionalRedissonAutoConfiguration : RedissonAutoConfigurationV2()
