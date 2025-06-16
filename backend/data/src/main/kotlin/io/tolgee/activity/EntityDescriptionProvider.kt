package io.tolgee.activity

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.data.EntityDescription
import io.tolgee.activity.data.EntityDescriptionWithRelations
import io.tolgee.model.EntityWithId
import io.tolgee.util.EntityUtil
import org.hibernate.proxy.HibernateProxy
import org.springframework.stereotype.Component
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaField

@Component
class EntityDescriptionProvider(
  private val entityUtil: EntityUtil,
) {
  fun getDescriptionWithRelations(entity: EntityWithId): EntityDescriptionWithRelations? {
    val description = getDescription(entity) ?: return null

    val relations = mutableMapOf<String, EntityDescriptionWithRelations>()

    getAnnotation(entity)?.paths?.forEach pathsForEach@{ path ->
      var realDescribingEntity: Any = entity
      path.split(".").forEach { pathItem ->
        val member = realDescribingEntity::class.members.find { it.name == pathItem }
        realDescribingEntity = member?.call(realDescribingEntity) ?: return@pathsForEach
      }

      (realDescribingEntity as? EntityWithId)?.let {
        relations[path] = getDescriptionWithRelations(it) ?: return@pathsForEach
      }
    }

    return EntityDescriptionWithRelations(
      description.entityClass,
      description.entityId,
      description.data,
      relations,
    )
  }

  private fun getAnnotation(entity: EntityWithId): ActivityEntityDescribingPaths? {
    if (entity is HibernateProxy) {
      return entity::class.superclasses.firstNotNullOfOrNull { it.findAnnotation<ActivityEntityDescribingPaths>() }
    }
    return entity::class.findAnnotation<ActivityEntityDescribingPaths>()
  }

  fun getDescription(entity: EntityWithId): EntityDescription? {
    val entityClass = entityUtil.getRealEntityClass(entity::class.java) ?: return null

    val fieldValues =
      entityClass.kotlin.members.filter { member ->
        member is KProperty<*> && member.javaField?.isAnnotationPresent(ActivityDescribingProp::class.java) ?: false
      }.associateTo(HashMap()) { it.name to it.call(entity) }

    return EntityDescription(
      entityClass.simpleName,
      entityId = entity.id,
      fieldValues,
    )
  }
}
