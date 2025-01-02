package io.tolgee.controllers.internal.e2eData

import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.executeInNewRepeatableTransaction
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import java.io.FileNotFoundException

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
  lateinit var testDataService: TestDataService

  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  open fun afterTestDataStored(data: TestDataBuilder) {}

  @GetMapping(value = ["/generate-standard"])
  @Transactional
  open fun generate(): StandardTestDataResult {
    val data = this.testData
    testDataService.saveTestData(data)
    afterTestDataStored(data)
    return getStandardResult(data)
  }

  fun getStandardResult(data: TestDataBuilder): StandardTestDataResult {
    return StandardTestDataResult(
      projects =
        data.data.projects.map {
          StandardTestDataResult.ProjectModel(name = it.self.name, id = it.self.id)
        },
      users =
        data.data.userAccounts.map {
          StandardTestDataResult.UserModel(name = it.self.name, username = it.self.username, id = it.self.id)
        },
      organizations =
        data.data.organizations.map {
          StandardTestDataResult.OrganizationModel(id = it.self.id, name = it.self.name, slug = it.self.slug)
        },
    )
  }

  @GetMapping(value = ["/clean"])
  open fun cleanup(): Any? {
    return tryUntilItDoesntBreakConstraint {
      executeInNewRepeatableTransaction(transactionManager) {
        entityManager.clear()
        try {
          testDataService.cleanTestData(this.testData)
        } catch (e: FileNotFoundException) {
          return@executeInNewRepeatableTransaction ResponseEntity.internalServerError().body(e.stackTraceToString())
        }
        return@executeInNewRepeatableTransaction null
      }
    }
  }

  data class StandardTestDataResult(
    val projects: List<ProjectModel>,
    val users: List<UserModel>,
    val organizations: List<OrganizationModel>,
  ) {
    data class UserModel(
      val name: String,
      val username: String,
      val id: Long,
    )

    data class ProjectModel(
      val name: String,
      val id: Long,
    )

    data class OrganizationModel(
      val id: Long,
      val slug: String,
      val name: String,
    )
  }
}
