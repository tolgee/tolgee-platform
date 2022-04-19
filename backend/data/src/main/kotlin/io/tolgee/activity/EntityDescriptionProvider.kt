package io.tolgee.activity

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.model.EntityWithId
import io.tolgee.util.EntityUtil
import org.springframework.stereotype.Component
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Component
class EntityDescriptionProvider(
  private val entityUtil: EntityUtil
) {
  fun getDescription(
    entity: EntityWithId,
  ): EntityDescription? {
    val entityClass = entityUtil.getRealEntityClass(entity::class.java) ?: return null

    val fieldValues = entityClass.kotlin.members.filter { member ->
      member.hasAnnotation<ActivityDescribingProp>()
    }.associateTo(HashMap()) { it.name to it.call(entity) }

    val description = mutableMapOf<String, EntityDescription>()

    entity::class.findAnnotation<ActivityEntityDescribingPaths>()?.paths?.forEach pathsForEach@{ path ->
      var realDescribingEntity: Any = entity
      path.split(".").forEach { pathItem ->
        val member = realDescribingEntity::class.members.find { it.name == pathItem }
        realDescribingEntity = member?.call(realDescribingEntity) ?: return@pathsForEach
      }

      (realDescribingEntity as? EntityWithId)?.let {
        description[path] = getDescription(it) ?: return@pathsForEach
      }
    }

    return EntityDescription(
      entityClass.simpleName,
      entity.id,
      fieldValues,
      description
    )
  }
}
