package io.tolgee.ee.api.v2.controllers.task

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class TaskControllerBranchingTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BranchMergeTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = BranchMergeTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS, Feature.BRANCHING)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists only tasks from selected branch and moved tasks`() {
    performProjectAuthGet("tasks?branch=${testData.mainBranch.name}")
      .andAssertThatJson {
        node("page.totalElements").isNumber.isEqualTo(BigDecimal(2))
        node("_embedded.tasks[0].number").isEqualTo(testData.mainTask.number)
        node("_embedded.tasks[0].branchName").isEqualTo(testData.mainBranch.name)
        node("_embedded.tasks[0].originBranchName").isNull()
        node("_embedded.tasks[1].number").isEqualTo(testData.mergedFeatureTask.number)
        node("_embedded.tasks[1].branchName").isEqualTo(testData.mainBranch.name)
        node("_embedded.tasks[1].originBranchName").isEqualTo(testData.featureBranch.name)
      }
  }
}
