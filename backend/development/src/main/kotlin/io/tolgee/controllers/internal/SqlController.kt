package io.tolgee.controllers.internal

import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@InternalController(["internal/sql"])
class SqlController(
  val entityManager: EntityManager,
) {
  @PostMapping(value = ["/list"])
  @Transactional
  fun getList(
    @RequestBody query: String,
  ): MutableList<Any?>? {
    return entityManager.createNativeQuery(query).resultList
  }

  @PostMapping(value = ["/execute"])
  @Transactional
  fun execute(
    @RequestBody query: String,
  ) {
    entityManager.createNativeQuery(query).executeUpdate()
  }
}
