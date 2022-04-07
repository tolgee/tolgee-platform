package io.tolgee.activity

import io.tolgee.activity.activities.common.ActivityProvider
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.holders.ActivityHolder
import io.tolgee.activity.propChangesProvider.PropChangesProvider
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.project_auth.ProjectHolder
import org.hibernate.EmptyInterceptor
import org.hibernate.collection.internal.AbstractPersistentCollection
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Component
class ActivityInterceptor : EmptyInterceptor() {

  @Autowired
  @org.springframework.context.annotation.Lazy
  lateinit var activityProvider: ActivityProvider

  @Autowired
  lateinit var applicationContext: ApplicationContext

  override fun onSave(
    entity: Any?,
    id: Serializable?,
    state: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ): Boolean {
    if (shouldHandleActivity(entity)) {
      onFieldModificationsActivity(entity, state, null, propertyNames, types, RevisionType.ADD)
    }
    return true
  }

  override fun onDelete(
    entity: Any?,
    id: Serializable?,
    state: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ) {
    onFieldModificationsActivity(entity, null, state, propertyNames, types, RevisionType.DEL)
  }

  override fun onFlushDirty(
    entity: Any?,
    id: Serializable?,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ): Boolean {
    onFieldModificationsActivity(entity, currentState, previousState, propertyNames, types, RevisionType.MOD)
    return true
  }

  override fun onCollectionRemove(collection: Any?, key: Serializable?) {
    onCollectionModification(collection, key)
  }

  override fun onCollectionRecreate(collection: Any?, key: Serializable?) {
    onCollectionModification(collection, key)
  }

  override fun onCollectionUpdate(collection: Any?, key: Serializable?) {
    onCollectionModification(collection, key)
  }

  private fun onCollectionModification(collection: Any?, key: Serializable?) {
    if (collection !is AbstractPersistentCollection || collection !is Collection<*> || key !is Long) {
      return
    }

    val collectionOwner = collection.owner
    if (collectionOwner !is EntityWithId
      || !collectionOwner::class.hasAnnotation<ActivityLoggedEntity>()) {
      return
    }

    val ownerField = collectionOwner::class.members.find {
      it.parameters.size == 1 && it.call(collectionOwner) === collection
    } ?: return

    val provider = getChangesProvider(collectionOwner, ownerField.name) ?: return

    val stored = (collection.storedSnapshot as? HashMap<*, *>)?.values?.toList()
    val changes = provider.getChanges(stored, collection) ?: return
    val activityModifiedEntity = getModifiedEntity(collectionOwner)

    val newChanges = activityModifiedEntity.modifications + mutableMapOf(ownerField.name to changes)
    activityModifiedEntity.modifications = (newChanges).toMutableMap()

    activityModifiedEntity.setEntityDescription(collectionOwner)
  }

  fun onFieldModificationsActivity(
    entity: Any?,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?,
    revisionType: RevisionType
  ) {
    if (!shouldHandleActivity(entity)) {
      return
    }

    entity as EntityWithId

    val activityModifiedEntity = getModifiedEntity(entity)

    val changesMap = getChangesMap(entity, currentState, previousState, propertyNames)

    activityModifiedEntity.revisionType = revisionType
    activityModifiedEntity.modifications.putAll(changesMap)

    activityModifiedEntity.setEntityDescription(entity)
  }

  private fun ActivityModifiedEntity.setEntityDescription(
    entity: EntityWithId,
  ) {
    val describingData = getChangeEntityDescription(entity, activityRevision)
    this.description = describingData.first?.filter { !this.modifications.keys.contains(it.key) }
    this.describingRelations = describingData.second
  }

  private fun getModifiedEntity(entity: EntityWithId): ActivityModifiedEntity {
    val activityModifiedEntity = activityHolder.modifiedEntities
      .computeIfAbsent(entity::class.simpleName!!) { mutableMapOf() }
      .computeIfAbsent(
        entity.id
      ) {
        ActivityModifiedEntity(
          activityRevision,
          entity::class.simpleName!!,
          entity.id
        )
      }

    activityRevision.meta = activityHolder.activity?.metaModifier?.let { modifier ->
      val meta = activityRevision.meta ?: activityHolder.meta
      modifier(meta, activityModifiedEntity, entity)
      meta
    }

    return activityModifiedEntity
  }

  private fun getChangeEntityDescription(
    entity: EntityWithId,
    activityRevision: ActivityRevision
  ): Pair<Map<String, Any?>?, Map<String, EntityDescriptionRef>?> {
    val rootDescription = applicationContext.getBean(EntityDescriptionProvider::class.java).getDescription(entity)
    val relations = rootDescription?.relations
      ?.map { it.key to compressRelation(it.value, activityRevision) }
      ?.toMap()

    return (rootDescription?.data to relations)
  }

  private fun compressRelation(value: EntityDescription, activityRevision: ActivityRevision): EntityDescriptionRef {
    val activityDescribingEntity = activityRevision.describingRelations
      .find { it.entityId == value.entityId && it.entityClass == value.entityClass }
      ?: let {
        val compressedRelations = value.relations.map { relation ->
          relation.key to compressRelation(relation.value, activityRevision)
        }.toMap()

        val activityDescribingEntity = ActivityDescribingEntity(
          activityRevision,
          value.entityClass,
          value.entityId
        ).also {
          it.data = value.data
          it.describingRelations = compressedRelations
        }
        activityRevision.describingRelations.add(activityDescribingEntity)
        activityDescribingEntity
      }
    return EntityDescriptionRef(activityDescribingEntity.entityClass, activityDescribingEntity.entityId)
  }

  private fun getChangesMap(
    entity: Any,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?
  ): Map<String, PropertyModification> {
    if (propertyNames == null) {
      return mapOf()
    }

    val changes = propertyNames.asSequence().mapIndexed { idx, name ->
      name to PropertyModification(previousState?.get(idx), currentState?.get(idx))
    }

    return changes.mapNotNull { (propertyName, change) ->
      val provider = getChangesProvider(entity, propertyName) ?: return@mapNotNull null
      propertyName to (provider.getChanges(change.old, change.new) ?: return@mapNotNull null)
    }.toMap()
  }

  fun getChangesProvider(entity: Any, propertyName: String): PropChangesProvider? {
    val propertyAnnotation = entity::class.members
      .find { it.name == propertyName }
      ?.findAnnotation<ActivityLoggedProp>()
      ?: return null

    val providerClass = propertyAnnotation.modificationProvider
    return applicationContext.getBean(providerClass.java)
  }

  fun shouldHandleActivity(entity: Any?): Boolean {
    return entity is EntityWithId && entity::class.hasAnnotation<ActivityLoggedEntity>()
  }

  private val activityRevision: ActivityRevision
    get() {
      var activityRevision = activityHolder.activityRevision

      if (activityRevision == null) {
        activityRevision = ActivityRevision().also { revision ->
          revision.authorId = userAccount?.id
          revision.projectId = projectHolder.project.id
          revision.type = activityHolder.activity?.type
        }
        activityHolder.activityRevision = activityRevision
      }

      return activityRevision
    }

  private val transactionTemplate: TransactionTemplate
    get() = applicationContext.getBean(TransactionTemplate::class.java)

  private val activityService: ActivityService
    get() = applicationContext.getBean(ActivityService::class.java)

  private val projectHolder: ProjectHolder
    get() = applicationContext.getBean(ProjectHolder::class.java)

  private val activityHolder: ActivityHolder
    get() {
      return try {
        applicationContext.getBean(ActivityHolder::class.java).also {
          // we must try to access something to get the exception thrown
          it.activityRevision
        }
      } catch (e: ScopeNotActiveException) {
        applicationContext.getBean("transactionActivityHolder", ActivityHolder::class.java)
      }
    }

  private val userAccount: UserAccountDto?
    get() = applicationContext.getBean(AuthenticationFacade::class.java).userAccountOrNull
}
