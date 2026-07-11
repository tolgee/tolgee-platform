package io.tolgee.activity

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.data.EntityDescription
import io.tolgee.activity.data.EntityDescriptionWithRelations
import io.tolgee.model.EntityWithId
import io.tolgee.util.EntityUtil
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField

@Component
class EntityDescriptionProvider(
  private val entityUtil: EntityUtil,
) {
  fun getDescriptionWithRelations(entity: EntityWithId): EntityDescriptionWithRelations? {
    val description = getDescription(entity) ?: return null

    val realClass = entityUtil.getRealEntityClass(entity::class.java) ?: return description.toWithRelations(emptyMap())
    val paths = describingPaths(realClass)
    if (paths.isEmpty()) return description.toWithRelations(emptyMap())

    val relations = HashMap<String, EntityDescriptionWithRelations>(paths.size)
    paths.forEach pathsForEach@{ path ->
      var realDescribingEntity: Any = entity
      path.steps.forEach { step ->
        val member = pathMember(realDescribingEntity::class.java, step) ?: return@pathsForEach
        realDescribingEntity = member.call(realDescribingEntity) ?: return@pathsForEach
      }
      (realDescribingEntity as? EntityWithId)?.let {
        relations[path.raw] = getDescriptionWithRelations(it) ?: return@pathsForEach
      }
    }

    return description.toWithRelations(relations)
  }

  fun getDescription(entity: EntityWithId): EntityDescription? {
    val entityClass = entityUtil.getRealEntityClass(entity::class.java) ?: return null
    val props = describingProps(entityClass)
    val fieldValues = HashMap<String, Any?>(props.size)
    for ((name, field) in props) {
      fieldValues[name] = field.get(entity)
    }
    return EntityDescription(
      entityClass.simpleName,
      entityId = entity.id,
      fieldValues,
    )
  }

  private fun describingPaths(entityClass: Class<*>): List<Path> =
    pathsCache.computeIfAbsent(entityClass) {
      it.kotlin
        .findAnnotation<ActivityEntityDescribingPaths>()
        ?.paths
        ?.map { p -> Path(p, p.split(".")) }
        ?: emptyList()
    }

  private fun describingProps(entityClass: Class<*>): List<Pair<String, Field>> =
    propsCache.computeIfAbsent(entityClass) {
      it.kotlin.members.mapNotNull { member ->
        if (member !is KProperty<*>) return@mapNotNull null
        val javaField = member.javaField ?: return@mapNotNull null
        if (!javaField.isAnnotationPresent(ActivityDescribingProp::class.java)) return@mapNotNull null
        javaField.isAccessible = true
        member.name to javaField
      }
    }

  private fun pathMember(
    clazz: Class<*>,
    name: String,
  ): KCallable<*>? {
    val perClass = pathMemberCache.computeIfAbsent(clazz) { ConcurrentHashMap() }
    return perClass
      .computeIfAbsent(name) {
        Optional(clazz.kotlin.members.find { m -> m.name == name })
      }.value
  }

  private fun EntityDescription.toWithRelations(relations: Map<String, EntityDescriptionWithRelations>) =
    EntityDescriptionWithRelations(entityClass, entityId, data, relations)

  private val pathsCache = ConcurrentHashMap<Class<*>, List<Path>>()
  private val propsCache = ConcurrentHashMap<Class<*>, List<Pair<String, Field>>>()
  private val pathMemberCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, Optional>>()

  private data class Path(
    val raw: String,
    val steps: List<String>,
  )

  // ConcurrentHashMap doesn't allow null values; wrap so we can cache "no member found".
  private data class Optional(
    val value: KCallable<*>?,
  )
}
