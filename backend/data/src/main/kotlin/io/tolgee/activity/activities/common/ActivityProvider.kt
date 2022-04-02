package io.tolgee.activity.activities.common

import org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Scope(SCOPE_SINGLETON)
@Component
class ActivityProvider(
  @Lazy
  private val activities: List<Activity>
) {

  private val typeActivityMap: Map<String, Activity> by lazy {
    activities.associateBy { it.type }
  }

  operator fun get(type: String): Activity? {
    return typeActivityMap[type]
  }
}
