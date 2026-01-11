package io.tolgee.util

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class EntityUtil(
  private val entityManager: EntityManager,
) {
  companion object {
    val REGEX = "\\$.*$".toRegex()
  }

  val cache = ConcurrentHashMap<String, Class<out Any>?>()

  fun getRealEntityClass(simpleName: String): Class<out Any>? {
    return cache.computeIfAbsent(simpleName) {
      val entitySimpleName = simpleName.replace(REGEX, "")
      entityManager
        .metamodel
        .entities
        .find { it.name == entitySimpleName }
        ?.javaType
    }
  }

  fun getRealEntityClass(maybeProxiedClass: Class<out Any>): Class<out Any>? {
    return getRealEntityClass(maybeProxiedClass.simpleName)
  }
}
