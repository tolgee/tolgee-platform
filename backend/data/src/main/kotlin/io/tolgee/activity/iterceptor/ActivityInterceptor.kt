package io.tolgee.activity.iterceptor

import io.sentry.Sentry
import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.RevisionType
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.hibernate.EmptyInterceptor
import org.hibernate.Transaction
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class ActivityInterceptor : EmptyInterceptor(), Logging {

  @Autowired
  lateinit var applicationContext: ApplicationContext

  override fun afterTransactionCompletion(tx: Transaction) {
    interceptedEventsManager.onAfterTransactionCompleted(tx)
  }

  override fun beforeTransactionCompletion(tx: Transaction) {
    if (tx.isActive) {
      val holder = this.applicationContext.getBean(ActivityHolder::class.java)
      if (!holder.enableAutoCompletion) {
        return
      }
      val activityRevision = holder.activityRevision
      if (!activityRevision.isInitializedByInterceptor && activityRevision.afterFlush == null) return
      logger.debug("Publishing project activity event")
      try {
        applicationContext.publishEvent(
          OnProjectActivityEvent(
            activityRevision,
            holder.modifiedEntities,
            holder.organizationId,
            holder.utmData,
            holder.sdkInfo
          )
        )
      } catch (e: Throwable) {
        Sentry.captureException(e)
        logger.error("Error publishing project activity event: ", e)
        holder.onBeforeTransactionCompletionError?.invoke(e)
        throw e
      }
    }
  }

  override fun onSave(
    entity: Any?,
    id: Serializable?,
    state: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ): Boolean {
    preCommitEventsPublisher.onPersist(entity)
    interceptedEventsManager.onFieldModificationsActivity(
      entity, state, null, propertyNames, RevisionType.ADD
    )
    return true
  }

  override fun onDelete(
    entity: Any?,
    id: Serializable?,
    state: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ) {
    preCommitEventsPublisher.onDelete(entity)
    interceptedEventsManager.onFieldModificationsActivity(
      entity, null, state, propertyNames, RevisionType.DEL
    )
  }

  override fun onFlushDirty(
    entity: Any?,
    id: Serializable?,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ): Boolean {
    preCommitEventsPublisher.onUpdate(entity)
    interceptedEventsManager.onFieldModificationsActivity(
      entity,
      currentState,
      previousState,
      propertyNames,
      RevisionType.MOD
    )
    return true
  }

  override fun onCollectionRemove(collection: Any?, key: Serializable?) {
    interceptedEventsManager.onCollectionModification(collection, key)
  }

  override fun onCollectionRecreate(collection: Any?, key: Serializable?) {
    interceptedEventsManager.onCollectionModification(collection, key)
  }

  override fun onCollectionUpdate(collection: Any?, key: Serializable?) {
    interceptedEventsManager.onCollectionModification(collection, key)
  }

  val interceptedEventsManager: InterceptedEventsManager
    get() = applicationContext.getBean(InterceptedEventsManager::class.java)

  val preCommitEventsPublisher: PreCommitEventPublisher
    get() = applicationContext.getBean(PreCommitEventPublisher::class.java)
}
