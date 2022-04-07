package io.tolgee.activity.holders

import io.tolgee.activity.ActivityService
import io.tolgee.activity.activities.common.Activity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import org.springframework.context.ApplicationContext
import javax.annotation.PreDestroy

open class ActivityHolder(
  private val applicationContext: ApplicationContext
) {
  open var activity: Activity? = null

  open var meta: MutableMap<String, Any?> = mutableMapOf()

  open var activityClass: Class<out Activity>?
    get() = activity?.let { it::class.java }
    set(value) {
      activity = applicationContext.getBean(value)
    }

  open var activityRevision: ActivityRevision? = null

  @PreDestroy
  open fun preDestroy() {
    applicationContext.getBean(ActivityService::class.java).storeData(this)
  }

  /**
   * This field stores all modified entities, it's stored before the transaction is committed
   */
  open var modifiedEntities: MutableMap<String, MutableMap<Long, ActivityModifiedEntity>> = mutableMapOf()
}
