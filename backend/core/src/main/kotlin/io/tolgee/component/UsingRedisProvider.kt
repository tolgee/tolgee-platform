package io.tolgee.component

import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class UsingRedisProvider(
  private val applicationContext: ApplicationContext,
) {
  val areWeUsingRedis: Boolean by lazy {
    try {
      applicationContext.getBean(StringRedisTemplate::class.java)
      true
    } catch (e: NoSuchBeanDefinitionException) {
      false
    }
  }
}
