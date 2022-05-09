package io.tolgee.activity

import io.sentry.Sentry
import io.tolgee.activity.data.ActivityType
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import javax.annotation.PreDestroy

open class ActivityHolder(
  private val applicationContext: ApplicationContext
) {
  open var activity: ActivityType? = null

  open var meta: MutableMap<String, Any?> = mutableMapOf()

  open var activityRevision: ActivityRevision? = null

  private val logger = LoggerFactory.getLogger(this::class.java)

  open var modifiedCollections: MutableMap<Pair<EntityWithId, String>, List<Any?>?> = mutableMapOf()

  @PreDestroy
  open fun preDestroy() {
    try {
      applicationContext.getBean(ActivityService::class.java).storeActivityData(this)
    } catch (e: Exception) {
      Sentry.captureException(e)
      logger.error(e.stackTraceToString())
    }
  }

  /**
   * This field stores all modified entities, it's stored before the transaction is committed
   */
  open var modifiedEntities: MutableMap<String, MutableMap<Long, ActivityModifiedEntity>> = mutableMapOf()
}
