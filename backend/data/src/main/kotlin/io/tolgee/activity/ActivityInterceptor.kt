package io.tolgee.activity

import io.tolgee.activity.activities.common.ActivityProvider
import io.tolgee.activity.holders.ActivityHolder
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.project_auth.ProjectHolder
import org.hibernate.EmptyInterceptor
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
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
      onActivity(entity, state, null, propertyNames, types, RevisionType.ADD)
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
    onActivity(entity, null, state, propertyNames, types, RevisionType.DEL)
  }

  override fun onFlushDirty(
    entity: Any?,
    id: Serializable?,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ): Boolean {
    onActivity(entity, currentState, previousState, propertyNames, types, RevisionType.MOD)
    return true
  }

  fun onActivity(
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

    transactionTemplate.execute {
      entity as EntityWithId

      val changesMap = getChangesMap(entity, currentState, previousState, propertyNames)

      val activityModifiedEntity = ActivityModifiedEntity(
        activityRevision,
        entity::class.simpleName!!,
        entity.id
      )

      activityRevision.meta = activityHolder.activity?.metaModifier?.let { modifier ->
        val meta = activityRevision.meta ?: activityHolder.meta
        modifier(meta, activityModifiedEntity, entity)
        meta
      }

      activityModifiedEntity.revisionType = revisionType
      activityModifiedEntity.modifications = changesMap.map { (property, change) ->
        property to PropertyModification(
          change.first,
          change.second
        )
      }.toMap()

      activityService.onActivity(activityModifiedEntity)
    }
  }

  private fun getChangesMap(
    entity: Any,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?
  ): Map<String, Pair<Any?, Any?>> {
    if (propertyNames == null) {
      return mapOf()
    }

    val changes = propertyNames.asSequence().mapIndexed { idx, name ->
      name to (previousState?.get(idx) to currentState?.get(idx))
    }

    return changes.filter { (propertyName, change) ->
      entity::class.members.find { it.name == propertyName }?.hasAnnotation<ActivityLogged>() == true &&
        change.first != change.second
    }.toMap()
  }

  fun shouldHandleActivity(entity: Any?): Boolean {
    return entity is EntityWithId && entity::class.hasAnnotation<ActivityLogged>()
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
    get() {
      return try {
        applicationContext.getBean(ProjectHolder::class.java).also {
          // we must try to access something to get the exception thrown
          it.project
        }
      } catch (e: ScopeNotActiveException) {
        return applicationContext.getBean("transactionProjectHolder", ProjectHolder::class.java)
      }
    }

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
