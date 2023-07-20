package io.tolgee.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.util.Logging
import org.springframework.context.ApplicationContext
import javax.annotation.PreDestroy
import kotlin.reflect.KClass

open class ActivityHolder(
  private val applicationContext: ApplicationContext
) : Logging {
  open var activity: ActivityType? = null

  open var meta: MutableMap<String, Any?> = mutableMapOf()

  open var activityRevision: ActivityRevision? = null

  open var modifiedCollections: MutableMap<Pair<EntityWithId, String>, List<Any?>?> = mutableMapOf()

  open var transactionRollbackOnly = false

  open var organizationId: Long? = null

  open var utmData: UtmData = null
  open var sdkInfo: Map<String, String?>? = null

  @PreDestroy
  open fun preDestroy() {
    if (!transactionRollbackOnly) {
      applicationContext.publishEvent(OnProjectActivityEvent(this))
    }
  }

  /**
   * This field stores all modified entities, it's stored before the transaction is committed
   */
  open var modifiedEntities:
    MutableMap<
      KClass<out EntityWithId>, MutableMap<Long, ActivityModifiedEntity>
      > = mutableMapOf()
}
