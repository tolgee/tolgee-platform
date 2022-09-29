package io.tolgee.controllers.internal.e2e_data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.service.OrganizationService
import io.tolgee.service.UserAccountService
import io.tolgee.service.project.ProjectService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import java.io.FileNotFoundException
import javax.persistence.EntityManager

abstract class AbstractE2eDataController {
  abstract val testData: TestDataBuilder

  @Autowired
  private lateinit var projectService: ProjectService

  @Autowired
  private lateinit var userAccountService: UserAccountService

  @Autowired
  private lateinit var organizationService: OrganizationService

  @Autowired
  private lateinit var entityManager: EntityManager

  @GetMapping(value = ["/clean"])
  @Transactional
  open fun cleanup(): Any? {
    try {
      testData.data.userAccounts.forEach {
        userAccountService.find(it.self.username)?.let { user ->
          userAccountService.delete(user)
        }
      }
      testData.data.organizations.forEach { organizationBuilder ->
        organizationBuilder.self.name.let { name -> organizationService.deleteAllByName(name) }
      }
    } catch (e: FileNotFoundException) {
      return ResponseEntity.internalServerError().body(e.stackTraceToString())
    }
    return null
  }
}
