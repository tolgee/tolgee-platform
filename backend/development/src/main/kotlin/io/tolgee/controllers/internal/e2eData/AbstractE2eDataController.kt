package io.tolgee.controllers.internal.e2eData

import io.tolgee.data.StandardTestDataResult
import io.tolgee.data.service.TestDataGeneratingService
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

  @Autowired
  private lateinit var testDataGeneratingService: TestDataGeneratingService

  open fun afterTestDataStored(data: TestDataBuilder) {}

  @GetMapping(value = ["/generate-standard"])
  @Transactional
  open fun generate(): StandardTestDataResult {
    return testDataGeneratingService.generate(testData, this::afterTestDataStored)
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
}
