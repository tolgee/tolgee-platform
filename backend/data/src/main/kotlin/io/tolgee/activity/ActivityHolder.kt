package io.tolgee.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.iterceptor.InterceptedEventsManager
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import jakarta.annotation.PreDestroy
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

open class ActivityHolder(val applicationContext: ApplicationContext) {
  open var activity: ActivityType? = null
    set(value) {
      field = value
      activityRevision.type = value
    }

  open var meta: MutableMap<String, Any?> = mutableMapOf()

  open val activityRevision: ActivityRevision by lazy {
    ActivityRevision()
  }

  open var modifiedCollections: MutableMap<Pair<EntityWithId, String>, List<Any?>?> = mutableMapOf()

  open var transactionRollbackOnly = false

  open var organizationId: Long? = null

  open var utmData: UtmData = null

  open var sdkInfo: Map<String, String?>? = null

  /**
   * This field stores all modified entities, it's stored before the transaction is committed
   */
  open var modifiedEntities:
    ModifiedEntitiesType = mutableMapOf()

  open var enableAutoCompletion: Boolean = true

  open var afterActivityFlushed: (() -> Unit)? = null
    set(value) {
      this.applicationContext.getBean(InterceptedEventsManager::class.java).initActivityHolder()
      field = value
    }

  var destroyer: (() -> Unit)? = null

  @PreDestroy
  fun destroy() {
    destroyer?.invoke()
  }
}

typealias ModifiedEntitiesType = MutableMap<KClass<out EntityWithId>, MutableMap<Long, ActivityModifiedEntity>>
