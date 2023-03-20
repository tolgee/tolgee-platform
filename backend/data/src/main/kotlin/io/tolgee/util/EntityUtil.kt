package io.tolgee.util

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class EntityUtil(
  private val entityManager: EntityManager
) {

  val cache = mutableMapOf<String, Class<out Any>?>()

  fun getRealEntityClass(simpleName: String): Class<out Any>? {
    return cache.computeIfAbsent(simpleName) {
      val entitySimpleName = simpleName.replace("\\$.*$".toRegex(), "")
      entityManager
        .metamodel
        .entities
        .find { it.name == entitySimpleName }?.javaType
    }
  }

  fun getRealEntityClass(maybeProxiedClass: Class<out Any>): Class<out Any>? {
    return getRealEntityClass(maybeProxiedClass.simpleName)
  }
}
