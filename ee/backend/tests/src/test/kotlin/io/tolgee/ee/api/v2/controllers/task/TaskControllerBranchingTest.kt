package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.util.Date

class TaskControllerBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BranchMergeTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var branchRepository: BranchRepository

  @BeforeEach
  fun setup() {
    testData = BranchMergeTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists tasks from archived merged branches only`() {
    testData.conflictsBranch.archivedAt = Date()
    branchRepository.save(testData.conflictsBranch)

    performProjectAuthGet("tasks?branch=${testData.mainBranch.name}")
      .andAssertThatJson {
        node("page.totalElements").isNumber.isEqualTo(BigDecimal(1))
        node("_embedded.tasks[0].number").isEqualTo(testData.conflictsBranchTask.number)
        node("_embedded.tasks[0].branchName").isEqualTo(testData.conflictsBranch.name)
      }
  }
}
