package io.tolgee.activity

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import kotlin.reflect.KClass

open class ActivityHolder {
  open var activity: ActivityType? = null
    set(value) {
      field = value
      activityRevision?.type = value
    }

  open var meta: MutableMap<String, Any?> = mutableMapOf()

  open val activityRevision: ActivityRevision by lazy {
    ActivityRevision()
  }

  open var modifiedCollections: MutableMap<Pair<EntityWithId, String>, List<Any?>?> = mutableMapOf()

  open var transactionRollbackOnly = false

  open var organizationId: Long? = null

  /**
   * This field stores all modified entities, it's stored before the transaction is committed
   */
  open var modifiedEntities:
    ModifiedEntitiesType = mutableMapOf()
}

typealias ModifiedEntitiesType = MutableMap<KClass<out EntityWithId>, MutableMap<Long, ActivityModifiedEntity>>
