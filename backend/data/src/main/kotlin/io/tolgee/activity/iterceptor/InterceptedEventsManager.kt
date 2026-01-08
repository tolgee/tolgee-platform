package io.tolgee.activity.iterceptor

import io.tolgee.activity.ActivityService
import io.tolgee.activity.EntityDescriptionProvider
import io.tolgee.activity.annotation.ActivityIgnoredProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.EntityDescriptionWithRelations
import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import io.tolgee.activity.propChangesProvider.PropChangesProvider
import io.tolgee.component.ActivityHolderProvider
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import org.apache.commons.lang3.exception.ExceptionUtils.getRootCause
import org.hibernate.Transaction
import org.hibernate.action.spi.BeforeTransactionCompletionProcess
import org.hibernate.collection.spi.AbstractPersistentCollection
import org.hibernate.event.spi.EventSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaField

@Component
@Scope(SCOPE_SINGLETON)
class InterceptedEventsManager(
  private val applicationContext: ApplicationContext,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  fun onAfterTransactionCompleted(tx: Transaction) {
    activityHolder.transactionRollbackOnly = tx.rollbackOnly
  }

  fun onCollectionModification(
    collection: Any?,
    key: Any?,
  ) {
    if (collection !is AbstractPersistentCollection<*> || collection !is Collection<*> || key !is Long) {
      return
    }

    val collectionOwner = collection.owner
    if (collectionOwner !is EntityWithId ||
      !collectionOwner::class.hasAnnotation<ActivityLoggedEntity>()
    ) {
      return
    }

    val ownerField =
      collectionOwner::class.members.find {
        it.parameters.size == 1 && it.callIfInitialized(collectionOwner) === collection
      } ?: return

    val provider = getChangesProvider(collectionOwner, ownerField.name) ?: return

    val stored = (collection.storedSnapshot as? HashMap<*, *>)?.values?.toList()

    val old =
      activityHolder.modifiedCollections.computeIfAbsent(collectionOwner to ownerField.name) {
        stored
      }

    val changes = provider.getChanges(old, collection) ?: return
    val activityModifiedEntity = getModifiedEntity(collectionOwner, RevisionType.MOD)

    val newChanges = activityModifiedEntity.modifications + mutableMapOf(ownerField.name to changes)
    activityModifiedEntity.modifications = (newChanges).toMutableMap()

    activityModifiedEntity.setEntityDescription(collectionOwner)
  }

  private fun KCallable<*>.callIfInitialized(instance: Any?): Any? {
    try {
      return this.call(instance)
    } catch (e: Exception) {
      if (getRootCause(e) is UninitializedPropertyAccessException) {
        return null
      }
      throw e
    }
  }

  fun onFieldModificationsActivity(
    entity: Any?,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
    revisionType: RevisionType,
  ) {
    if (!shouldHandleActivity(entity)) {
      return
    }

    entity as EntityWithId

    if (isAllIgnored(entity, currentState, previousState, propertyNames)) {
      return
    }

    val activityModifiedEntity = getModifiedEntity(entity, revisionType)

    val changesMap = getChangesMap(entity, currentState, previousState, propertyNames)

    activityModifiedEntity.revisionType = revisionType
    activityModifiedEntity.modifications.putAll(changesMap)

    activityModifiedEntity.setEntityDescription(entity)
  }

  private fun isAllIgnored(
    entity: EntityWithId,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
  ): Boolean {
    val ignoredFields = getEntityIgnoredMembers(entity)

    return propertyNames?.foldIndexed(true) { index, acc, current ->
      if (currentState?.get(index) !== previousState?.get(index) && !ignoredFields.contains(current)) {
        return@foldIndexed false
      }
      return@foldIndexed acc
    } ?: true
  }

  private fun ActivityModifiedEntity.setEntityDescription(entity: EntityWithId) {
    val describingData = getChangeEntityDescription(entity, activityRevision)
    this.describingData = describingData.first?.filter { !this.modifications.keys.contains(it.key) }
    this.describingRelations = describingData.second
  }

  private fun getModifiedEntity(
    entity: EntityWithId,
    revisionType: RevisionType,
  ): ActivityModifiedEntity {
    val activityModifiedEntity =
      activityHolder.modifiedEntities
        .computeIfAbsent(entity::class) { mutableMapOf() }
        .computeIfAbsent(
          entity.id,
        ) {
          ActivityModifiedEntity(
            activityRevision,
            entity::class.simpleName!!,
            entity.id,
          ).also { it.revisionType = revisionType }
        }

    return activityModifiedEntity
  }

  private fun getChangeEntityDescription(
    entity: EntityWithId,
    activityRevision: ActivityRevision,
  ): Pair<Map<String, Any?>?, Map<String, EntityDescriptionRef>?> {
    val rootDescription =
      applicationContext.getBean(EntityDescriptionProvider::class.java).getDescriptionWithRelations(
        entity,
      )
    val relations =
      rootDescription
        ?.relations
        ?.map { it.key to compressRelation(it.value, activityRevision) }
        ?.toMap()

    return (rootDescription?.data to relations)
  }

  private fun compressRelation(
    value: EntityDescriptionWithRelations,
    activityRevision: ActivityRevision,
  ): EntityDescriptionRef {
    val activityDescribingEntity =
      activityHolder
        .getDescribingRelationFromCache(value.entityId, value.entityClass) {
          val compressedRelations =
            value.relations
              .map { relation ->
                relation.key to compressRelation(relation.value, activityRevision)
              }.toMap()

          val activityDescribingEntity =
            ActivityDescribingEntity(
              activityRevision,
              value.entityClass,
              value.entityId,
            )

          activityDescribingEntity.data = value.data
          activityDescribingEntity.describingRelations = compressedRelations
          activityRevision.describingRelations.add(activityDescribingEntity)

          activityDescribingEntity
        }
    return EntityDescriptionRef(activityDescribingEntity.entityClass, activityDescribingEntity.entityId)
  }

  private fun getChangesMap(
    entity: Any,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
  ): Map<String, PropertyModification> {
    if (propertyNames == null) {
      return mapOf()
    }

    return propertyNames
      .asSequence()
      .mapIndexedNotNull { idx, propertyName ->
        val old = previousState?.get(idx)
        val new = currentState?.get(idx)
        val provider = getChangesProvider(entity, propertyName) ?: return@mapIndexedNotNull null
        propertyName to (provider.getChanges(old, new) ?: return@mapIndexedNotNull null)
      }.toMap()
  }

  fun getChangesProvider(
    entity: Any,
    propertyName: String,
  ): PropChangesProvider? {
    val propertyAnnotation = getEntityAnnotatedMembers(entity)[propertyName] ?: return null
    val providerClass = propertyAnnotation.modificationProvider
    return applicationContext.getBean(providerClass.java)
  }

  private fun getEntityAnnotatedMembers(entity: Any): Map<String, ActivityLoggedProp> {
    return annotatedMembersCache.computeIfAbsent(entity::class.java) {
      entity::class
        .members
        .mapNotNull {
          if (it !is KProperty<*>) return@mapNotNull null
          val annotation = it.javaField?.getAnnotation(ActivityLoggedProp::class.java) ?: return@mapNotNull null
          it.name to annotation
        }.toMap()
    }
  }

  private fun getEntityIgnoredMembers(entity: Any): Set<String> {
    return ignoredMembersCache.computeIfAbsent(entity::class.java) {
      entity::class
        .members
        .filter {
          it is KProperty<*> && (it.javaField?.isAnnotationPresent(ActivityIgnoredProp::class.java) ?: false)
        }.map { it.name }
        .toSet()
    }
  }

  private val annotatedMembersCache: ConcurrentHashMap<Class<*>, Map<String, ActivityLoggedProp>> = ConcurrentHashMap()
  private val ignoredMembersCache: ConcurrentHashMap<Class<*>, Set<String>> = ConcurrentHashMap()

  private fun shouldHandleActivity(entity: Any?) =
    entity is EntityWithId &&
      entity::class.hasAnnotation<ActivityLoggedEntity>() &&
      !entity.disableActivityLogging

  private val activityRevision: ActivityRevision
    get() {
      initActivityHolder()
      return activityHolder.activityRevision
    }

  fun initActivityHolder() {
    if (!activityHolder.activityRevision.isInitializedByInterceptor) {
      initializeActivityRevision()
      registerBeforeCompletion()
    }
  }

  private fun initializeActivityRevision() {
    activityHolder.activityRevision.also { revision ->
      ActivityRevisionInitializer(applicationContext, revision, activityHolder).initialize()
    }
  }

  private fun registerBeforeCompletion() {
    entityManager.unwrap(EventSource::class.java).actionQueue.registerProcess(
      BeforeTransactionCompletionProcess {
        if (it.transaction.isActive) {
          if (!activityHolder.enableAutoCompletion) {
            return@BeforeTransactionCompletionProcess
          }
          val activityRevision = activityHolder.activityRevision
          if (!activityRevision.isInitializedByInterceptor) {
            return@BeforeTransactionCompletionProcess
          }
          logger.debug("Publishing project activity event")
          try {
            publishOnActivityEvent(activityRevision)
            entityManager.flush()
            entityManager.clear()
            activityService.storeActivityData(activityRevision, activityHolder.modifiedEntities)
            activityHolder.afterActivityFlushed?.invoke()
          } catch (e: Throwable) {
            logger.error("Error while publishing project activity event", e)
            throw e
          }
        }
      },
    )
  }

  private fun publishOnActivityEvent(activityRevision: ActivityRevision) {
    applicationContext.publishEvent(
      OnProjectActivityEvent(
        activityRevision,
        activityHolder.modifiedEntities,
        activityHolder.utmData,
        activityHolder.businessEventData,
      ),
    )
  }

  private val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  private val activityService: ActivityService by lazy {
    applicationContext.getBean(ActivityService::class.java)
  }

  private val activityHolderProvider: ActivityHolderProvider by lazy {
    applicationContext.getBean(ActivityHolderProvider::class.java)
  }

  private val activityHolder
    get() = activityHolderProvider.getActivityHolder()
}
