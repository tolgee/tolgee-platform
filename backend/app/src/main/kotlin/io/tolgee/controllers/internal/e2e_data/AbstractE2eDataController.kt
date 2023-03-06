package io.tolgee.controllers.internal.e2e_data

import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
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

  @Autowired
  private lateinit var testDataService: TestDataService

  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

  @GetMapping(value = ["/generate-standard"])
  @Transactional
  open fun generate(): StandardTestDataResult {
    val data = this.testData
    testDataService.saveTestData(data)
    return StandardTestDataResult(
      projects = data.data.projects.map {
        StandardTestDataResult.ProjectModel(name = it.self.name, id = it.self.id)
      },
      users = data.data.userAccounts.map {
        StandardTestDataResult.UserModel(name = it.self.name, username = it.self.username, id = it.self.id)
      }
    )
  }

  @GetMapping(value = ["/clean"])
  open fun cleanup(): Any? {
    return tryUntilItDoesntBreakConstraint {
      return@tryUntilItDoesntBreakConstraint executeInNewTransaction(transactionManager) {
        try {
          testData.data.userAccounts.forEach {
            userAccountService.findActive(it.self.username)?.let { user ->
              userAccountService.delete(user)
            }
          }
          testData.data.organizations.forEach { organizationBuilder ->
            organizationBuilder.self.name.let { name -> organizationService.deleteAllByName(name) }
          }
        } catch (e: FileNotFoundException) {
          return@executeInNewTransaction ResponseEntity.internalServerError().body(e.stackTraceToString())
        }
        return@executeInNewTransaction null
      }
    }
  }

  data class StandardTestDataResult(
    val projects: List<ProjectModel>,
    val users: List<UserModel>
  ) {
    data class UserModel(
      val name: String,
      val username: String,
      val id: Long
    )

    data class ProjectModel(
      val name: String,
      val id: Long
    )
  }
}
