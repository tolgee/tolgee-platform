package io.tolgee.activity.iterceptor

import io.tolgee.activity.data.RevisionType
import io.tolgee.util.Logging
import org.hibernate.Interceptor
import org.hibernate.Transaction
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ActivityDatabaseInterceptor :
  Interceptor,
  Logging {
  @Autowired
  lateinit var applicationContext: ApplicationContext

  override fun afterTransactionCompletion(tx: Transaction) {
    interceptedEventsManager.onAfterTransactionCompleted(tx)
  }

  override fun onSave(
    entity: Any?,
    id: Any?,
    state: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?,
  ): Boolean {
    preCommitEventsPublisher.onPersist(entity)
    interceptedEventsManager.onFieldModificationsActivity(
      entity,
      state,
      null,
      propertyNames,
      RevisionType.ADD,
    )
    return true
  }

  override fun onDelete(
    entity: Any?,
    id: Any?,
    state: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?,
  ) {
    preCommitEventsPublisher.onDelete(entity)
    interceptedEventsManager.onFieldModificationsActivity(
      entity,
      null,
      state,
      propertyNames,
      RevisionType.DEL,
    )
  }

  override fun onFlushDirty(
    entity: Any?,
    id: Any?,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?,
  ): Boolean {
    preCommitEventsPublisher.onUpdate(entity, previousState, propertyNames)
    interceptedEventsManager.onFieldModificationsActivity(
      entity,
      currentState,
      previousState,
      propertyNames,
      RevisionType.MOD,
    )
    return true
  }

  override fun onCollectionRemove(
    collection: Any?,
    key: Any?,
  ) {
    interceptedEventsManager.onCollectionModification(collection, key)
  }

  override fun onCollectionRecreate(
    collection: Any?,
    key: Any?,
  ) {
    interceptedEventsManager.onCollectionModification(collection, key)
  }

  override fun onCollectionUpdate(
    collection: Any?,
    key: Any?,
  ) {
    interceptedEventsManager.onCollectionModification(collection, key)
  }

  val interceptedEventsManager: InterceptedEventsManager by lazy {
    applicationContext.getBean(InterceptedEventsManager::class.java)
  }
  val preCommitEventsPublisher: PreCommitEventPublisher by lazy {
    applicationContext.getBean(PreCommitEventPublisher::class.java)
  }
}
