package io.tolgee.activity

import io.tolgee.activity.holders.ActivityHolder
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.Activity
import io.tolgee.model.ActivityRevision
import io.tolgee.model.EntityWithId
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.project_auth.ProjectHolder
import org.hibernate.EmptyInterceptor
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.io.Serializable
import kotlin.reflect.full.hasAnnotation

@Component
class ActivityInterceptor : EmptyInterceptor() {

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

      val activity = Activity(
        activityRevision,
        entity::class.simpleName!!,
        entity.id
      )

      activityRevision.meta = holder.manager?.metaModifier?.let {
        val meta = activityRevision.meta ?: mutableMapOf()
        it(meta, activity, entity)
        meta
      }

      activity.revisionType = revisionType
      activity.oldValues = changesMap.map { (property, change) -> property to change.first }.toMap()
      activity.newValues = changesMap.map { (property, change) -> property to change.second }.toMap()

      activityService.onActivity(activity)
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

  private val holder: ActivityHolder
    get() {
      return applicationContext.getBean(ActivityHolder::class.java)
    }

  private val activityRevision: ActivityRevision
    get() {
      var activityRevision = holder.activityRevision

      if (activityRevision == null) {
        activityRevision = ActivityRevision().also { revision ->
          revision.authorId = userAccount?.id
          revision.projectId = projectHolder.project.id
          revision.activityManager = holder.manager?.let { it::class.simpleName }
        }
        holder.activityRevision = activityRevision
      }

      return activityRevision
    }

  private val transactionTemplate: TransactionTemplate
    get() = applicationContext.getBean(TransactionTemplate::class.java)

  private val activityService: ActivityService
    get() = applicationContext.getBean(ActivityService::class.java)

  private val projectHolder: ProjectHolder
    get() = applicationContext.getBean(ProjectHolder::class.java)

  private val userAccount: UserAccountDto?
    get() = applicationContext.getBean(AuthenticationFacade::class.java).userAccountOrNull
}
