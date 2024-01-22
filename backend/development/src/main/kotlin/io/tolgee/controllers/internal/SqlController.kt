package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/sql"])
@Transactional
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
